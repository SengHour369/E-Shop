package com.example.eshop.ai.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.poifs.filesystem.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.zip.*;

/** Streams XLSX worksheet XML; never evaluates formulas or follows workbook relationships. */
@Component
public class ExcelImportParser {

    private final int maxBytes, maxRows, maxColumns, maxSheets;
    private final Semaphore slots = new Semaphore(2);

    public record ParsedRow(
            int number,
            Map<String, String> values
    ) {}

    public record ParsedSheet(
            String name,
            List<String> headers,
            List<ParsedRow> rows
    ) {}

    public record ParsedFile(
            String filename,
            byte[] bytes,
            List<ParsedSheet> sheets
    ) {}

    public ExcelImportParser(
            @Value("${ai.import.max-bytes:5242880}") int maxBytes,
            @Value("${ai.import.max-rows:10000}") int maxRows,
            @Value("${ai.import.max-columns:64}") int maxColumns,
            @Value("${ai.import.max-sheets:16}") int maxSheets
    ) {
        this.maxBytes = maxBytes;
        this.maxRows = maxRows;
        this.maxColumns = maxColumns;
        this.maxSheets = maxSheets;
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(40L * 1024 * 1024);
        ZipSecureFile.setMaxFileCount(256);
    }

    public ParsedFile parse(MultipartFile file) {
        if (!slots.tryAcquire()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Workbook parser busy"
            );
        }

        Path temp = null;

        try {
            String name = Objects.requireNonNullElse(
                    file.getOriginalFilename(),
                    ""
            );

            if (!name.toLowerCase(Locale.ROOT).matches(".*\\.xlsx?$")
                    || file.isEmpty()
                    || file.getSize() > maxBytes) {
                throw invalid();
            }

            byte[] bytes;

            try (InputStream input = file.getInputStream()) {
                bytes = input.readNBytes(maxBytes + 1);
            }

            if (bytes.length > maxBytes) {
                throw invalid();
            }

            List<ParsedSheet> sheets;

            if (name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                preflight(bytes);
                temp = Files.createTempFile("eshop-import-", ".xlsx");
                Files.write(temp, bytes);
                sheets = xlsx(temp);
            } else {
                try (POIFSFileSystem fs = new POIFSFileSystem(
                        new ByteArrayInputStream(bytes)
                )) {
                    rejectVba(fs.getRoot());
                }

                try (Workbook workbook = WorkbookFactory.create(
                        new ByteArrayInputStream(bytes)
                )) {
                    if (!(workbook instanceof org.apache.poi.hssf.usermodel.HSSFWorkbook)) {
                        throw invalid();
                    }

                    sheets = legacy(workbook);
                }
            }

            if (sheets.isEmpty()
                    || sheets.stream().mapToInt(s -> s.rows().size()).sum() == 0) {
                throw invalid();
            }

            return new ParsedFile(
                    name.replaceAll("[^A-Za-z0-9._-]", "_"),
                    bytes,
                    sheets
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid();
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
            slots.release();
        }
    }

    private void rejectVba(DirectoryEntry dir) {
        for (Entry entry : dir) {
            if (entry.getName().toLowerCase(Locale.ROOT).contains("vba")
                    || entry.getName().equals("EncryptedPackage")) {
                throw invalid();
            }

            if (entry instanceof DirectoryEntry child) {
                rejectVba(child);
            }
        }
    }

    private void preflight(byte[] bytes) throws IOException {
        long total = 0;
        int entries = 0;
        Set<String> names = new HashSet<>();

        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(bytes)
        )) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase(Locale.ROOT);

                if (++entries > 256
                        || !names.add(name)
                        || name.contains("vba")
                        || name.contains("externallinks")
                        || name.contains("embeddings")) {
                    throw invalid();
                }

                int n;

                while ((n = zip.read(buffer)) != -1) {
                    total += n;

                    if (total > 40L * 1024 * 1024) {
                        throw invalid();
                    }
                }
            }
        }

        if (entries == 0) {
            throw invalid();
        }
    }

    private List<ParsedSheet> xlsx(Path path) throws Exception {
        List<ParsedSheet> result = new ArrayList<>();
        int[] total = {0, 0};

        try (OPCPackage pkg = OPCPackage.open(
                path.toFile(),
                PackageAccess.READ
        )) {
            if (pkg.getParts().stream().anyMatch(p ->
                    p.getContentType().toLowerCase(Locale.ROOT).contains("macroenabled")
                            || p.getContentType().toLowerCase(Locale.ROOT).contains("macrosheet"))) {
                throw invalid();
            }

            XSSFReader reader = new XSSFReader(pkg);
            SharedStrings strings = reader.getSharedStringsTable();
            var sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

            while (sheets.hasNext()) {
                try (InputStream input = sheets.next()) {
                    if (result.size() >= maxSheets) {
                        throw invalid();
                    }

                    SheetBuilder builder = new SheetBuilder(
                            sheets.getSheetName(),
                            total
                    );
                    XMLReader xml = XMLHelper.newXMLReader();
                    xml.setContentHandler(new DefaultHandler() {
                        int column, row;
                        String type;
                        boolean text;
                        StringBuilder value = new StringBuilder();
                        Map<Integer, String> cells = new TreeMap<>();

                        @Override
                        public void startElement(
                                String uri,
                                String local,
                                String q,
                                Attributes attrs
                        ) {
                            switch (local) {
                                case "row" -> {
                                    row = Integer.parseInt(attrs.getValue("r"));
                                    cells.clear();
                                }
                                case "c" -> {
                                    column = new CellReference(
                                            attrs.getValue("r")
                                    ).getCol();

                                    if (column >= maxColumns) {
                                        throw invalid();
                                    }

                                    type = attrs.getValue("t");
                                    value.setLength(0);
                                }
                                case "f" -> throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Formulas are not accepted; upload literal values"
                                );
                                case "v", "t" -> text = true;
                            }
                        }

                        @Override
                        public void characters(
                                char[] ch,
                                int start,
                                int length
                        ) {
                            if (text) {
                                value.append(ch, start, length);

                                if (value.length() > 2000) {
                                    throw invalid();
                                }
                            }
                        }

                        @Override
                        public void endElement(
                                String uri,
                                String local,
                                String q
                        ) {
                            if (local.equals("v") || local.equals("t")) {
                                text = false;
                            }

                            if (local.equals("c")) {
                                String v = value.toString();

                                if ("s".equals(type)) {
                                    v = strings
                                            .getItemAt(Integer.parseInt(v))
                                            .getString();
                                }

                                if ((type == null || "n".equals(type))
                                        && !v.isBlank()) {
                                    v = new java.math.BigDecimal(v)
                                            .stripTrailingZeros()
                                            .toPlainString();
                                }

                                if ("b".equals(type)) {
                                    v = v.equals("1") ? "true" : "false";
                                }

                                if ("e".equals(type) || v.length() > 2000) {
                                    throw invalid();
                                }

                                cells.put(column, v);
                            }

                            if (local.equals("row")) {
                                builder.add(row, cells);
                            }
                        }
                    });
                    xml.parse(new InputSource(input));
                    result.add(builder.finish());
                }
            }
        }

        return result;
    }

    private List<ParsedSheet> legacy(Workbook workbook) {
        if (workbook.getNumberOfSheets() > maxSheets) {
            throw invalid();
        }

        List<ParsedSheet> result = new ArrayList<>();
        int[] total = {0, 0};

        for (Sheet sheet : workbook) {
            if (sheet.getLastRowNum() > maxRows) {
                throw invalid();
            }

            SheetBuilder builder = new SheetBuilder(
                    sheet.getSheetName(),
                    total
            );

            for (Row row : sheet) {
                Map<Integer, String> cells = new TreeMap<>();

                for (Cell cell : row) {
                    if (cell.getColumnIndex() >= maxColumns
                            || cell.getCellType() == CellType.FORMULA
                            || cell.getCellType() == CellType.ERROR) {
                        throw invalid();
                    }

                    String v = switch (cell.getCellType()) {
                        case STRING -> cell.getStringCellValue();
                        case BOOLEAN -> Boolean.toString(
                                cell.getBooleanCellValue()
                        );
                        case NUMERIC -> java.math.BigDecimal
                                .valueOf(cell.getNumericCellValue())
                                .stripTrailingZeros()
                                .toPlainString();
                        default -> "";
                    };

                    if (v.length() > 2000) {
                        throw invalid();
                    }

                    cells.put(cell.getColumnIndex(), v);
                }

                builder.add(row.getRowNum() + 1, cells);
            }

            result.add(builder.finish());
        }

        return result;
    }

    private class SheetBuilder {

        final String name;
        final int[] total;
        final List<String> headers = new ArrayList<>();
        final List<ParsedRow> rows = new ArrayList<>();

        SheetBuilder(String name, int[] total) {
            this.name = name;
            this.total = total;
        }

        void add(int number, Map<Integer, String> cells) {
            for (String value : cells.values()) {
                total[1] += value.length();

                if (total[1] > 10_000_000) {
                    throw invalid();
                }
            }

            if (cells.values().stream().allMatch(String::isBlank)) {
                return;
            }

            if (headers.isEmpty()) {
                for (int i = 0; i <= Collections.max(cells.keySet()); i++) {
                    String h = cells.getOrDefault(i, "").trim();

                    if (h.isBlank()
                            || headers.contains(h)
                            || h.length() > 120) {
                        throw invalid();
                    }

                    headers.add(h);
                }
            } else {
                if (++total[0] > maxRows || number > maxRows + 1) {
                    throw invalid();
                }

                Map<String, String> values = new LinkedHashMap<>();
                cells.forEach((i, v) -> {
                    if (i >= headers.size()) {
                        throw invalid();
                    }

                    values.put(headers.get(i), v);
                });
                rows.add(new ParsedRow(number, values));
            }
        }

        ParsedSheet finish() {
            return new ParsedSheet(name, headers, rows);
        }
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported, malformed, encrypted or oversized workbook"
        );
    }
}
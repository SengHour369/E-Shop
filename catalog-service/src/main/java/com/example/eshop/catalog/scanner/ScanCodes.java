package com.example.eshop.catalog.scanner;

import com.example.eshop.common.exception.BusinessLogicException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Normalizes a client-decoded scanner value. This class does not decode camera images. */
public final class ScanCodes {
    public static final int MAX_STORED = 64;
    private static final Set<Integer> GTIN_LENGTHS = Set.of(8, 12, 13);

    private ScanCodes() {}

    public record Normalized(ScanFormat format, String code, List<String> barcodeCandidates, List<String> skuCandidates) {}

    /** Blank becomes null. A value is stored without spaces and in upper case. */
    public static String storedBarcode(String raw) {
        if (raw == null) {
            return null;
        }
        String compact = raw.trim().replace(" ", "");
        if (compact.isEmpty()) {
            return null;
        }
        if (compact.length() > MAX_STORED) {
            throw new BusinessLogicException("Barcode is too long");
        }
        if (!compact.matches("[0-9A-Za-z.$/+%-]+")) {
            throw new BusinessLogicException("Barcode contains unsupported characters");
        }
        return compact.toUpperCase(Locale.ROOT);
    }

    public static Normalized normalize(String raw, ScanFormat requested) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessLogicException("Scanner code is required");
        }
        if (raw.length() > 512) {
            throw new BusinessLogicException("Scanner code is too long");
        }
        if (raw.chars().anyMatch(ch -> Character.isISOControl(ch))) {
            throw new BusinessLogicException("Scanner code contains unsupported characters");
        }
        String compact = raw.trim().replace(" ", "");
        ScanFormat format = requested != null ? requested : infer(compact);
        return switch (format) {
            case EAN_13 -> gtin(format, digits(compact, "EAN-13"), 13);
            case EAN_8 -> gtin(format, digits(compact, "EAN-8"), 8);
            case UPC_A -> gtin(format, digits(compact, "UPC-A"), 12);
            case UPC_E -> upcE(compact);
            case CODE_39 -> symbolic(format, compact, true);
            case CODE_128, SKU -> symbolic(format, compact, false);
            case QR_CODE -> qr(compact);
        };
    }

    public static int checkDigit(String digitsWithoutCheck) {
        int sum = 0;
        for (int i = digitsWithoutCheck.length() - 1, fromRight = 1; i >= 0; i--, fromRight++) {
            int digit = digitsWithoutCheck.charAt(i) - '0';
            sum += digit * (fromRight % 2 == 1 ? 3 : 1);
        }
        return (10 - (sum % 10)) % 10;
    }

    /** Expands an 8-digit UPC-E (number system, six digits, check) to UPC-A. */
    public static String expandUpcE(String eight) {
        if (eight == null || eight.length() != 8 || !eight.chars().allMatch(Character::isDigit)) {
            throw new BusinessLogicException("UPC-E must be 8 digits");
        }
        String data = eight.substring(1, 7);
        char last = data.charAt(5);
        String body;
        if (last == '0' || last == '1' || last == '2') {
            body = "" + data.charAt(0) + data.charAt(1) + last + "0000" + data.charAt(2) + data.charAt(3) + data.charAt(4);
        } else if (last == '3') {
            body = "" + data.charAt(0) + data.charAt(1) + data.charAt(2) + "00000" + data.charAt(3) + data.charAt(4);
        } else if (last == '4') {
            body = "" + data.charAt(0) + data.charAt(1) + data.charAt(2) + data.charAt(3) + "00000" + data.charAt(4);
        } else {
            body = data.substring(0, 5) + "0000" + last;
        }
        String withoutCheck = eight.charAt(0) + body;
        return withoutCheck + checkDigit(withoutCheck);
    }

    private static Normalized gtin(ScanFormat format, String digits, int length) {
        if (digits.length() != length) {
            throw new BusinessLogicException(format.name().replace('_', '-') + " must be " + length + " digits");
        }
        requireCheck(digits);
        LinkedHashSet<String> barcodes = new LinkedHashSet<>();
        barcodes.add(digits);
        addGtinEquivalents(barcodes, digits);
        return new Normalized(format, digits, List.copyOf(barcodes), List.of());
    }

    private static Normalized upcE(String compact) {
        String digits = digits(compact, "UPC-E");
        if (digits.length() != 8) {
            throw new BusinessLogicException("UPC-E must be 8 digits");
        }
        String expanded = expandUpcE(digits);
        if (digits.charAt(7) != expanded.charAt(11)) {
            throw new BusinessLogicException("Barcode check digit is invalid");
        }
        LinkedHashSet<String> barcodes = new LinkedHashSet<>();
        barcodes.add(digits);
        barcodes.add(expanded);
        barcodes.add("0" + expanded);
        return new Normalized(ScanFormat.UPC_E, digits, List.copyOf(barcodes), List.of());
    }

    private static Normalized symbolic(ScanFormat format, String compact, boolean code39) {
        String value = compact.toUpperCase(Locale.ROOT);
        if (value.length() > MAX_STORED) {
            throw new BusinessLogicException("Scanner code is too long");
        }
        String pattern = code39 ? "[0-9A-Z.$/+% -]+" : "[0-9A-Z.$/+%-]+";
        if (!value.matches(pattern)) {
            throw new BusinessLogicException("Scanner code contains unsupported characters");
        }
        LinkedHashSet<String> barcodes = new LinkedHashSet<>();
        LinkedHashSet<String> skus = new LinkedHashSet<>();
        barcodes.add(value);
        skus.add(value);
        if (value.chars().allMatch(Character::isDigit) && GTIN_LENGTHS.contains(value.length()) && validCheck(value)) {
            addGtinEquivalents(barcodes, value);
        }
        return new Normalized(format, value, List.copyOf(barcodes), List.copyOf(skus));
    }

    private static Normalized qr(String compact) {
        LinkedHashSet<String> barcodes = new LinkedHashSet<>();
        LinkedHashSet<String> skus = new LinkedHashSet<>();
        for (String token : tokens(compact)) {
            String value = token.trim().replace(" ", "");
            if (value.isEmpty() || value.length() > MAX_STORED) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            if (!upper.matches("[0-9A-Z.$/+%-]+")) {
                continue;
            }
            if (upper.chars().allMatch(Character::isDigit) && GTIN_LENGTHS.contains(upper.length())) {
                if (!validCheck(upper)) {
                    continue;
                }
                barcodes.add(upper);
                addGtinEquivalents(barcodes, upper);
            } else {
                barcodes.add(upper);
                skus.add(upper);
            }
        }
        if (barcodes.isEmpty() && skus.isEmpty()) {
            String digits = compact.replace("-", "");
            if (digits.chars().allMatch(Character::isDigit) && GTIN_LENGTHS.contains(digits.length())) {
                throw new BusinessLogicException("Barcode check digit is invalid");
            }
            throw new BusinessLogicException("QR code did not contain a supported product code");
        }
        String primary = barcodes.stream().findFirst().orElseGet(() -> skus.iterator().next());
        return new Normalized(ScanFormat.QR_CODE, primary, List.copyOf(barcodes), List.copyOf(skus));
    }

    private static List<String> tokens(String value) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        tokens.add(value);
        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                URI uri = URI.create(value);
                if (uri.getPath() != null) {
                    for (String part : uri.getPath().split("/")) {
                        if (!part.isBlank()) {
                            tokens.add(URLDecoder.decode(part, StandardCharsets.UTF_8));
                        }
                    }
                }
                if (uri.getRawQuery() != null) {
                    for (String pair : uri.getRawQuery().split("&")) {
                        int eq = pair.indexOf('=');
                        String rawValue = eq >= 0 ? pair.substring(eq + 1) : pair;
                        tokens.add(URLDecoder.decode(rawValue, StandardCharsets.UTF_8));
                    }
                }
            } catch (IllegalArgumentException ignored) {
                tokens.add(value);
            }
        }
        return new ArrayList<>(tokens);
    }

    private static ScanFormat infer(String compact) {
        String digits = compact.replace("-", "");
        if (digits.chars().allMatch(Character::isDigit)) {
            if (digits.length() == 13) return ScanFormat.EAN_13;
            if (digits.length() == 12) return ScanFormat.UPC_A;
            if (digits.length() == 8) return ScanFormat.EAN_8;
        }
        if (compact.chars().anyMatch(Character::isLetter) || compact.contains("-")) {
            return ScanFormat.SKU;
        }
        return ScanFormat.CODE_128;
    }

    private static String digits(String compact, String label) {
        String digits = compact.replace("-", "");
        if (!digits.chars().allMatch(Character::isDigit)) {
            throw new BusinessLogicException(label + " must contain only digits");
        }
        return digits;
    }

    private static void requireCheck(String digits) {
        if (!validCheck(digits)) {
            throw new BusinessLogicException("Barcode check digit is invalid");
        }
    }

    private static void addGtinEquivalents(LinkedHashSet<String> barcodes, String digits) {
        if (digits.length() == 12) {
            barcodes.add("0" + digits);
        }
        if (digits.length() == 13 && digits.startsWith("0")) {
            barcodes.add(digits.substring(1));
        }
    }

    private static boolean validCheck(String digits) {
        if (digits.length() < 2) {
            return false;
        }
        int expected = checkDigit(digits.substring(0, digits.length() - 1));
        return digits.charAt(digits.length() - 1) - '0' == expected;
    }
}

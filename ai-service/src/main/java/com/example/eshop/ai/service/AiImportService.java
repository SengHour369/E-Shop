package com.example.eshop.ai.service;

import com.example.eshop.ai.model.*;
import com.example.eshop.ai.repository.*;
import com.example.eshop.ai.client.*;
import com.example.eshop.common.imports.*;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.*;
import com.example.eshop.common.request.RequestIds;
import com.example.eshop.common.notification.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiImportService {

    private final AiImportBatchRepository batches;
    private final AiImportFileRepository files;
    private final AiImportImageRepository images;
    private final AiImportSheetRepository sheets;
    private final AiImportColumnMappingRepository mappings;
    private final AiImportRowRepository rows;
    private final AiImportValidationErrorRepository errors;
    private final AiImportRevisionRepository revisions;
    private final AiImportApprovalRepository approvals;
    private final AiImportExecutionRepository executions;
    private final AiImportRowExecutionRepository rowExecutions;
    private final AiImportAuditHistoryRepository history;
    private final NotificationOutboxRepository outbox;
    private final LivePermissionService permissions;
    private final ImportDomainClient domain;
    private final AuthToolClient auth;
    private final ImportMappingClient mappingClient;
    private final ImportApprovalProof proof;
    private final ObjectMapper mapper;
    private final AuditLogService audit;

    public record Preview(
            AiImportBatch batch,
            List<AiImportSheet> sheets,
            List<AiImportColumnMapping> mappings,
            Page<AiImportRow> rows,
            List<AiImportValidationError> errors,
            Summary summary,
            List<AiImportImageRepository.Metadata> images
    ) {}

    public record Summary(
            long totalRows,
            long editedRows,
            Map<String, Long> statuses
    ) {}

    public record RegistryItem(
            String name,
            String permission,
            Map<String, ImportTool.Field> schema,
            String handler,
            boolean confirmationRequired,
            String risk,
            boolean enabled,
            String unavailableReason
    ) {}

    public List<RegistryItem> registry() {
        var actor = permissions.current();

        if (!actor.permits("IMPORT_UPLOAD")
                && !actor.permits("IMPORT_REVIEW")) {
            throw new AccessDeniedException("Import permission required");
        }

        return Arrays.stream(ImportTool.values())
                .map(t -> new RegistryItem(
                        t.name(),
                        t.permission,
                        t.schema,
                        t.domainTool()
                                ? "catalog-service:" + t.name()
                                : "ai-service:" + t.name(),
                        t.domainTool()
                                || Set.of(
                                ImportTool.IMPORT_SUBMIT,
                                ImportTool.IMPORT_APPROVE,
                                ImportTool.IMPORT_REJECT,
                                ImportTool.IMPORT_REQUEST_CHANGES
                        ).contains(t),
                        t.highRisk
                                ? "HIGH_RISK_WRITE"
                                : Set.of(
                                ImportTool.IMPORT_VALIDATE,
                                ImportTool.IMPORT_PREVIEW,
                                ImportTool.IMPORT_EXECUTION_STATUS
                        ).contains(t)
                                ? "READ_ONLY"
                                : "LOW_RISK_WRITE",
                        t.enabled,
                        t.unavailableReason()
                ))
                .toList();
    }

    public void require(String permission) {
        if (!permissions.current().permits(permission)) {
            throw new AccessDeniedException(
                    "Required import permission missing"
            );
        }
    }

    public static String token() {
        if (org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes attributes) {
            String value = attributes.getRequest()
                    .getHeader("Authorization");

            if (value != null && value.startsWith("Bearer ")) {
                return value;
            }
        }

        throw new AccessDeniedException("Authentication required");
    }

    @Transactional
    public Map<String, Object> stage(ImportContracts.Row row) {
        require("IMPORT_UPLOAD");
        require(row.tool().permission);
        var headers = new ArrayList<>(row.values().keySet());
        byte[] bytes;

        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var output = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Draft");
            var header = sheet.createRow(0);
            var values = sheet.createRow(1);

            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
                values.createCell(i).setCellValue(
                        row.values().get(headers.get(i))
                );
            }

            workbook.write(output);
            bytes = output.toByteArray();
        } catch (java.io.IOException invalid) {
            throw bad("Draft could not be prepared");
        }

        var batch = upload(new ApprovalStagingData.ParsedFile(
                "assistant-draft.xlsx",
                bytes,
                List.of(new ApprovalStagingData.ParsedSheet(
                        "Draft",
                        headers,
                        List.of(new ApprovalStagingData.ParsedRow(
                                2,
                                row.values()
                        ))
                ))
        ));
        var sheet = sheets.findByBatchId(batch.getId()).get(0);
        var columns = new LinkedHashMap<String, String>();
        headers.forEach(header -> columns.put(header, header));
        batch = confirmMapping(
                batch.getId(),
                sheet.getId(),
                batch.getRevision(),
                row.tool(),
                columns
        );
        batch.setSourceType("AI_ASSISTANT");
        batch.setEntityType(
                row.tool().name().startsWith("PRODUCT_SKU")
                        ? "PRODUCT_SKU"
                        : row.tool().name().split("_BULK")[0]
        );
        batch.setEntityId(row.values().get("id"));
        return Map.of(
                "batchId", batch.getId().toString(),
                "revision", batch.getRevision(),
                "status", batch.getStatus(),
                "toolType", row.tool().name()
        );
    }

    @Transactional
    public AiImportBatch stageProduct(
            ImportContracts.Row row,
            List<org.springframework.web.multipart.MultipartFile> attachments
    ) {
        if (row == null
                || row.tool() == null
                || !Set.of(
                ImportTool.PRODUCT_BULK_CREATE,
                ImportTool.PRODUCT_BULK_UPDATE
        ).contains(row.tool())) {
            throw bad("Product create/update tool required");
        }

        if (row.values() == null
                || row.values().size() > 64
                || !row.tool().schema.keySet()
                .containsAll(row.values().keySet())
                || row.values().entrySet().stream().anyMatch(e ->
                e.getValue() == null
                        || e.getValue().length() > (
                        e.getKey().equals("skus") ? 12000 : 2000
                ))) {
            throw bad("Invalid product fields");
        }

        if (row.values().containsKey("imageIds")
                || row.values().containsKey("imageChecksums")) {
            throw bad(
                    "Attach actual image files; image references cannot be supplied"
            );
        }

        if (attachments != null && attachments.size() > 4) {
            throw bad("At most four product images are allowed");
        }

        var staged = stage(row);
        var batch = batches.findById(
                UUID.fromString(staged.get("batchId").toString())
        ).orElseThrow();
        batch.setSourceType(
                row.tool() == ImportTool.PRODUCT_BULK_CREATE
                        ? "MANUAL_CREATE"
                        : "MANUAL_UPDATE"
        );
        batch.setFilename("Manual product request");
        var first = rows.findByBatchId(batch.getId()).get(0);

        if (attachments != null) {
            for (var file : attachments) {
                batch = attachImage(
                        batch.getId(),
                        first.getId(),
                        batch.getRevision(),
                        file
                );
            }
        }

        record(
                batch,
                "MANUAL_PRODUCT_STAGED",
                Map.of("tool", row.tool().name())
        );
        return batch;
    }

    @Transactional
    public AiImportBatch upload(ApprovalStagingData.ParsedFile parsed) {
        require("IMPORT_UPLOAD");
        AiImportBatch batch = new AiImportBatch();
        batch.setOwnerId(CurrentActor.userId());
        batch.setLastEditorId(CurrentActor.userId());
        batch.setFilename(parsed.filename());

        try {
            batch.setChecksum(
                    HexFormat.of().formatHex(
                            java.security.MessageDigest
                                    .getInstance("SHA-256")
                                    .digest(parsed.bytes())
                    )
            );
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        batch.setRequestId(RequestIds.current());
        batch.setTraceId(MDC.get("traceId"));
        batches.saveAndFlush(batch);

        AiImportFile file = new AiImportFile();
        file.setBatchId(batch.getId());
        file.setFilename(parsed.filename());
        file.setContent(parsed.bytes());
        files.save(file);

        for (var parsedSheet : parsed.sheets()) {
            AiImportSheet sheet = new AiImportSheet();
            sheet.setBatchId(batch.getId());
            sheet.setName(parsedSheet.name());
            sheet.setHeaders(json(parsedSheet.headers()));
            sheets.save(sheet);

            AiImportColumnMapping mapping = new AiImportColumnMapping();
            mapping.setBatchId(batch.getId());
            mapping.setSheetId(sheet.getId());
            mappings.save(mapping);

            for (var parsedRow : parsedSheet.rows()) {
                AiImportRow row = new AiImportRow();
                row.setBatchId(batch.getId());
                row.setSheetId(sheet.getId());
                row.setRowNumber(parsedRow.number());
                row.setOriginalValues(json(parsedRow.values()));
                rows.save(row);
            }
        }

        record(batch, "UPLOADED", null);
        return batch;
    }

    @Transactional(readOnly = true)
    public Page<AiImportBatch> list(int page, int size, boolean review) {
        return list(page, size, review, false);
    }

    @Transactional(readOnly = true)
    public Page<AiImportBatch> list(
            int page,
            int size,
            boolean review,
            boolean assigned
    ) {
        require(review || assigned ? "IMPORT_REVIEW" : "IMPORT_UPLOAD");
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (permissions.current().administrator()) {
            return review
                    ? batches.findByStatus("SUBMITTED", pageable)
                    : batches.findAll(pageable);
        }

        if (!review && assigned) {
            return batches.findByAssignedReviewerId(
                    CurrentActor.userId(),
                    pageable
            );
        }

        return review
                ? batches.findByAssignedReviewerIdAndStatus(
                CurrentActor.userId(),
                "SUBMITTED",
                pageable
        )
                : batches.findByOwnerId(CurrentActor.userId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<AiImportBatch> list(
            int page,
            int size,
            boolean review,
            boolean assigned,
            boolean executedOnly
    ) {
        if (!executedOnly) {
            return list(page, size, review, assigned);
        }

        require(assigned ? "IMPORT_REVIEW" : "IMPORT_UPLOAD");
        var actor = permissions.current();
        org.springframework.data.jpa.domain.Specification<AiImportBatch> scope =
                (root, query, builder) -> {
                    var states = root.get("status").in(List.of(
                            "PROCESSING",
                            "COMPLETED",
                            "PARTIAL_SUCCESS",
                            "FAILED"
                    ));

                    if (actor.administrator()) {
                        return states;
                    }

                    return builder.and(
                            states,
                            builder.equal(
                                    root.get(
                                            assigned
                                                    ? "assignedReviewerId"
                                                    : "ownerId"
                                    ),
                                    actor.userId()
                            )
                    );
                };
        return batches.findAll(
                scope,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> requests(
            int page,
            int size,
            boolean mine,
            String search,
            String status,
            String source,
            String entity
    ) {
        require(mine ? "IMPORT_UPLOAD" : "IMPORT_REVIEW");

        if (page < 0 || size < 1 || size > 100 || search.length() > 120) {
            throw bad("Invalid request filters");
        }

        var actor = permissions.current();
        org.springframework.data.jpa.domain.Specification<AiImportBatch> scope =
                (root, query, builder) -> {
                    var terms = new ArrayList<
                            jakarta.persistence.criteria.Predicate
                            >();

                    if (mine) {
                        terms.add(builder.equal(
                                root.get("ownerId"),
                                actor.userId()
                        ));
                    } else if (!actor.administrator()) {
                        terms.add(builder.equal(
                                root.get("assignedReviewerId"),
                                actor.userId()
                        ));
                    }

                    if (!source.isBlank()) {
                        terms.add(builder.equal(
                                root.get("sourceType"),
                                source
                        ));
                    }

                    if (!entity.isBlank()) {
                        terms.add(builder.equal(
                                root.get("entityType"),
                                entity
                        ));
                    }

                    if (!search.isBlank()) {
                        String term = "%"
                                + search.toLowerCase(Locale.ROOT)
                                .replace("%", "\\%")
                                .replace("_", "\\_")
                                + "%";
                        var matchingRows = query.subquery(UUID.class);
                        var row = matchingRows.from(AiImportRow.class);
                        matchingRows.select(row.get("batchId"))
                                .where(builder.like(
                                        builder.lower(
                                                row.get("valuesJson")
                                        ),
                                        term,
                                        '\\'
                                ));
                        terms.add(builder.or(
                                builder.like(
                                        builder.lower(
                                                root.get("filename")
                                        ),
                                        term,
                                        '\\'
                                ),
                                builder.like(
                                        root.get("id").as(String.class),
                                        term
                                ),
                                builder.like(
                                        root.get("entityId"),
                                        term
                                ),
                                root.get("id").in(matchingRows)
                        ));
                    }

                    return builder.and(terms.toArray(
                            jakarta.persistence.criteria.Predicate[]::new
                    ));
                };
        var counts = new LinkedHashMap<String, Long>();
        counts.put("ALL", batches.count(scope));

        for (String value : List.of(
                "DRAFT",
                "SUBMITTED",
                "CHANGES_REQUESTED",
                "APPROVED",
                "REJECTED",
                "COMPLETED",
                "PARTIAL_SUCCESS",
                "FAILED",
                "INVALID",
                "VALIDATED",
                "PROCESSING"
        )) {
            counts.put(
                    value,
                    batches.count(scope.and(
                            (root, query, builder) ->
                                    builder.equal(root.get("status"), value)
                    ))
            );
        }

        var filtered = status.isBlank()
                ? scope
                : scope.and((root, query, builder) ->
                builder.equal(root.get("status"), status));
        var result = batches.findAll(
                filtered,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
        return Map.of(
                "items", result.getContent(),
                "total", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "counts", counts
        );
    }

    @Transactional(readOnly = true)
    public Preview preview(UUID id, int page, int size, String status) {
        AiImportBatch batch = read(id);
        var pageRows = rows.page(id, status, PageRequest.of(page, size));
        Set<UUID> ids = new HashSet<>();
        pageRows.forEach(r -> ids.add(r.getId()));
        Map<String, Long> counts = new TreeMap<>();
        long total = 0, edited = 0;

        for (var count : rows.summarize(id)) {
            counts.put(count.getStatus(), count.getRowCount());
            total += count.getRowCount();
            edited += count.getEditedCount();
        }

        return new Preview(
                batch,
                sheets.findByBatchId(id),
                mappings.findByBatchId(id),
                pageRows,
                errors.findByBatchId(id).stream()
                        .filter(e -> ids.contains(e.getRowId()))
                        .toList(),
                new Summary(total, edited, counts),
                images.metadata(id)
        );
    }

    private AiImportBatch read(UUID id) {
        AiImportBatch batch = batches.findById(id)
                .orElseThrow(AiImportService::notFound);
        access(batch);
        return batch;
    }

    private AiImportBatch lock(UUID id, int version) {
        AiImportBatch batch = batches.lock(id)
                .orElseThrow(AiImportService::notFound);
        access(batch);

        if (batch.getRevision() != version) {
            throw conflict("Batch changed; reload its current version");
        }

        return batch;
    }

    private void access(AiImportBatch batch) {
        var actor = permissions.current();

        if (actor.userId() == batch.getOwnerId()
                && actor.permits("IMPORT_UPLOAD")) {
            return;
        }

        if (!actor.permits("IMPORT_REVIEW")
                || !actor.administrator()
                && !Objects.equals(
                batch.getAssignedReviewerId(),
                actor.userId()
        )) {
            throw new AccessDeniedException("Import unavailable");
        }
    }

    private void editable(AiImportBatch batch) {
        long actor = CurrentActor.userId();

        if (actor == batch.getOwnerId()
                && Set.of("SUBMITTED", "APPROVED")
                .contains(batch.getStatus())) {
            throw conflict(
                    "Pending and approved revisions are locked. "
                            + "Wait for Return to Maker."
            );
        }

        if (actor != batch.getOwnerId()
                && (!permissions.current().permits("IMPORT_REVIEW")
                || !Objects.equals(
                batch.getAssignedReviewerId(),
                actor
        ))) {
            throw new AccessDeniedException(
                    "Only the maker or assigned reviewer may edit this request"
            );
        }

        if (Set.of(
                "PROCESSING",
                "COMPLETED",
                "PARTIAL_SUCCESS",
                "FAILED",
                "REJECTED"
        ).contains(batch.getStatus())) {
            throw conflict("This batch is immutable; create a new import");
        }
    }

    private void changed(
            AiImportBatch batch,
            String action,
            Object detail
    ) {
        editable(batch);
        batch.setRevision(batch.getRevision() + 1);
        batch.setValidatedRevision(null);
        batch.setApprovedRevision(null);
        batch.setReviewerId(null);
        batch.setStatus("DRAFT");
        batch.setLastEditorId(CurrentActor.userId());
        errors.deleteByBatchId(batch.getId());
        rows.findByBatchId(batch.getId())
                .forEach(r -> r.setStatus(
                        r.isSkipped() ? "SKIPPED" : "UNVALIDATED"
                ));
        record(batch, action, detail);
    }

    @Transactional
    public AiImportBatch analyze(UUID id, int version) {
        AiImportBatch batch = lock(id, version);
        changed(batch, "ANALYZED", null);

        for (AiImportSheet sheet : sheets.findByBatchId(id)) {
            List<String> headers = parseHeaders(sheet.getHeaders());
            ImportTool tool = detect(sheet.getName());
            sheet.setTool(tool == null ? null : tool.name());
            sheet.setConfirmed(false);
            AiImportColumnMapping mapping = mapping(id, sheet.getId());
            mapping.setSuggestions(
                    json(
                            tool == null
                                    ? Map.of()
                                    : mappingClient.suggest(tool, headers)
                    )
            );
        }

        return batch;
    }

    private ImportTool detect(String name) {
        String n = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");

        if (n.contains("update") && n.contains("categor")) {
            return ImportTool.CATEGORY_BULK_UPDATE;
        }
        if (n.contains("update") && n.contains("branch")) {
            return ImportTool.BRANCH_BULK_UPDATE;
        }
        if (n.contains("update") && n.contains("product")
                && !n.contains("sku")) {
            return ImportTool.PRODUCT_BULK_UPDATE;
        }
        if (n.contains("categor")) return ImportTool.CATEGORY_BULK_CREATE;
        if (n.contains("branch")) return ImportTool.BRANCH_BULK_CREATE;
        if (n.contains("sku")) return ImportTool.PRODUCT_SKU_BULK_CREATE;
        if (n.contains("promotion")) return ImportTool.PROMOTION_BULK_CREATE;
        if (n.contains("adjust")) {
            return ImportTool.INVENTORY_STOCK_ADJUST_BULK;
        }
        if (n.contains("inventor") || n.contains("stock")) {
            return ImportTool.INVENTORY_STOCK_IN_BULK;
        }
        if (n.contains("product")) return ImportTool.PRODUCT_BULK_CREATE;

        return null;
    }

    @Transactional
    public AiImportBatch confirmMapping(
            UUID id,
            UUID sheetId,
            int version,
            ImportTool tool,
            Map<String, String> columns
    ) {
        AiImportBatch batch = lock(id, version);
        editable(batch);

        if (tool == null || !tool.domainTool() || !tool.enabled) {
            throw bad("Unavailable import type");
        }

        require(tool.permission);
        AiImportSheet sheet = sheets.findById(sheetId)
                .filter(s -> s.getBatchId().equals(id))
                .orElseThrow(AiImportService::notFound);

        if (columns == null
                || columns.isEmpty()
                || !parseHeaders(sheet.getHeaders())
                .containsAll(columns.keySet())
                || !tool.schema.keySet().containsAll(columns.values())
                || new HashSet<>(columns.values()).size()
                != columns.size()) {
            throw bad("Invalid or ambiguous mapping");
        }

        if (Set.of(
                ImportTool.PRODUCT_BULK_CREATE,
                ImportTool.PRODUCT_BULK_UPDATE
        ).contains(tool)
                && columns.values().stream()
                .anyMatch(f ->
                        f.equals("imageIds")
                                || f.equals("imageChecksums")
                )) {
            throw bad(
                    "Image references are managed by the attachment workflow"
            );
        }

        for (var field : tool.schema.entrySet()) {
            if (field.getValue().required()
                    && !columns.containsValue(field.getKey())) {
                throw bad("Missing required mapping: " + field.getKey());
            }
        }

        changed(batch, "MAPPING_CONFIRMED", Map.of(
                "sheetId", sheetId,
                "tool", tool,
                "columns", columns
        ));
        sheet.setTool(tool.name());
        String entityType = tool.name().startsWith("PRODUCT_SKU")
                ? "PRODUCT_SKU"
                : tool.name().split("_BULK")[0];
        batch.setEntityType(
                batch.getEntityType() == null
                        || batch.getEntityType().equals(entityType)
                        ? entityType
                        : "MULTIPLE"
        );
        sheet.setConfirmed(true);
        mapping(id, sheetId).setConfirmedValues(json(columns));

        for (AiImportRow row : rows.findByBatchId(id)) {
            if (row.getSheetId().equals(sheetId)) {
                Map<String, String> original = values(
                        row.getOriginalValues()
                );
                Map<String, String> mapped = new LinkedHashMap<>();

                if (original.isEmpty() && row.isEdited()) {
                    // Manually added rows have no workbook cells to remap.
                    continue;
                }

                columns.forEach((header, field) -> {
                    String value = original.get(header);

                    if (value != null) {
                        mapped.put(field, value);
                    }
                });
                row.setValuesJson(json(mapped));
            }
        }

        return batch;
    }

    public record AddedRow(
            AiImportBatch batch,
            AiImportRow row
    ) {}

    @org.springframework.beans.factory.annotation.Value("${ai.import.max-rows:10000}")
    private int maxImportRows = 10000;

    @Transactional
    public AddedRow addRow(
            UUID id,
            UUID sheetId,
            int version,
            UUID rowId,
            Map<String, String> values
    ) {
        AiImportBatch batch = lock(id, version);
        editable(batch);
        AiImportSheet sheet = sheets.findById(sheetId)
                .filter(s -> s.getBatchId().equals(id))
                .orElseThrow(AiImportService::notFound);

        if (!sheet.isConfirmed()) {
            throw bad("Confirm the worksheet mapping before adding rows");
        }

        ImportTool tool = tool(sheet);
        require(tool.permission);

        if (rowId == null
                || values == null
                || values.size() > 64
                || !tool.schema.keySet().containsAll(values.keySet())
                || values.containsKey("imageIds")
                || values.containsKey("imageChecksums")
                || values.values().stream().anyMatch(value ->
                value == null
                        || value.length() > 2000
                        || value.chars().anyMatch(
                        Character::isISOControl
                ))) {
            throw bad("Invalid row fields");
        }

        if (rows.existsById(rowId)) {
            throw conflict(
                    "This row has already been saved; reload the workbook"
            );
        }

        List<AiImportRow> existing = rows.findByBatchId(id);

        if (existing.size() >= maxImportRows) {
            throw bad("Workbook row limit reached");
        }

        int number = existing.stream()
                .mapToInt(AiImportRow::getRowNumber)
                .max()
                .orElse(1) + 1;

        changed(batch, "ROW_ADDED", Map.of(
                "rowId", rowId,
                "sheetId", sheetId,
                "values", values
        ));

        AiImportRow row = new AiImportRow();
        row.setId(rowId);
        row.setBatchId(id);
        row.setSheetId(sheetId);
        row.setRowNumber(number);
        row.setOriginalValues("{}");
        row.setValuesJson(json(values));
        row.setEdited(true);
        rows.save(row);

        return new AddedRow(batch, row);
    }

    @Transactional
    public AiImportBatch edit(
            UUID id,
            UUID rowId,
            int version,
            Map<String, String> values
    ) {
        AiImportBatch batch = lock(id, version);
        editable(batch);
        AiImportRow row = rows.findById(rowId)
                .filter(r -> r.getBatchId().equals(id))
                .orElseThrow(AiImportService::notFound);
        ImportTool tool = tool(
                sheets.findById(row.getSheetId()).orElseThrow()
        );
        require(tool.permission);

        if (values == null
                || values.size() > 64
                || values.values().stream().anyMatch(v ->
                v == null || v.length() > 12000)
                || !tool.schema.keySet().containsAll(values.keySet())) {
            throw bad("Invalid row fields");
        }

        if (Set.of(
                ImportTool.PRODUCT_BULK_CREATE,
                ImportTool.PRODUCT_BULK_UPDATE
        ).contains(tool)) {
            Map<String, String> current = values(row.getValuesJson());
            Map<String, String> updated = new LinkedHashMap<>(values);

            for (String field : List.of("imageIds", "imageChecksums")) {
                if (values.containsKey(field)
                        && !Objects.equals(
                        values.get(field),
                        current.get(field)
                )) {
                    throw bad(
                            "Image references are managed by the attachment workflow"
                    );
                }

                if (current.containsKey(field)) {
                    updated.put(field, current.get(field));
                }
            }

            values = updated;
        }

        changed(batch, "ROW_EDITED", Map.of(
                "rowId", rowId,
                "before", row.getValuesJson(),
                "after", values
        ));
        row.setValuesJson(json(values));
        row.setEdited(true);
        return batch;
    }

    @Transactional
    public AiImportBatch validate(UUID id, int version) {
        AiImportBatch batch = lock(id, version);
        editable(batch);
        validateData(batch);
        return batch;
    }

    @Transactional
    public AiImportBatch skip(
            UUID id,
            UUID rowId,
            int version,
            boolean skipped
    ) {
        AiImportBatch batch = lock(id, version);
        editable(batch);
        AiImportRow row = rows.findById(rowId)
                .filter(r -> r.getBatchId().equals(id))
                .orElseThrow(AiImportService::notFound);
        changed(batch, "ROW_SKIP_CHANGED", Map.of(
                "rowId", rowId,
                "skipped", skipped
        ));
        row.setSkipped(skipped);
        row.setStatus(skipped ? "SKIPPED" : "UNVALIDATED");
        return batch;
    }

    private boolean validateData(AiImportBatch batch) {
        UUID id = batch.getId();
        errors.deleteByBatchId(id);

        Map<UUID, AiImportSheet> bySheet = new HashMap<>();
        sheets.findByBatchId(id).forEach(s -> bySheet.put(s.getId(), s));

        Map<String, AiImportRow> seen = new HashMap<>();
        boolean valid;
        List<AiImportRow> allRows = rows.findByBatchId(id);
        PermissionSnapshot grants = permissions.current();
        Map<UUID, ImportContracts.Validation> domainResults = new HashMap<>();

        List<AiImportRow> eligible = allRows.stream()
                .filter(r -> !r.isSkipped())
                .filter(r -> {
                    AiImportSheet s = bySheet.get(r.getSheetId());

                    if (!s.isConfirmed() || s.getTool() == null) {
                        return false;
                    }

                    ImportTool t = tool(s);

                    if (!grants.permits(t.permission)) {
                        throw new AccessDeniedException(
                                "Required domain permission missing"
                        );
                    }

                    return t.validate(values(r.getValuesJson())).isEmpty()
                            && imageErrors(batch, r, t).isEmpty();
                })
                .toList();

        for (int offset = 0; offset < eligible.size(); offset += 500) {
            List<AiImportRow> chunk = eligible.subList(
                    offset,
                    Math.min(offset + 500, eligible.size())
            );

            try {
                List<ImportContracts.Row> data = chunk.stream()
                        .map(r -> new ImportContracts.Row(
                                tool(bySheet.get(r.getSheetId())),
                                values(r.getValuesJson())
                        ))
                        .toList();
                var result = domain.validateBulk(token(), data);

                if (result == null || result.size() != chunk.size()) {
                    continue;
                }

                for (int i = 0; i < chunk.size(); i++) {
                    if (result.get(i) != null) {
                        domainResults.put(
                                chunk.get(i).getId(),
                                result.get(i)
                        );
                    }
                }
            } catch (Exception ignored) {
                /* These rows remain invalid until the authoritative service is available. */
            }
        }

        valid = allRows.stream().anyMatch(r -> !r.isSkipped());

        for (AiImportRow row : allRows) {
            if (row.isSkipped()) {
                row.setStatus("SKIPPED");
                continue;
            }

            row.setStatus("VALID");
            row.setAffectedResourceId(null);
            List<String> messages = new ArrayList<>();
            AiImportSheet sheet = bySheet.get(row.getSheetId());

            if (!sheet.isConfirmed() || sheet.getTool() == null) {
                messages.add("MAPPING_CONFIRMATION_REQUIRED");
            } else {
                ImportTool tool = tool(sheet);

                if (!grants.permits(tool.permission)) {
                    throw new AccessDeniedException(
                            "Required domain permission missing"
                    );
                }

                Map<String, String> v = values(row.getValuesJson());
                messages.addAll(tool.validate(v));
                messages.addAll(imageErrors(batch, row, tool));

                if (messages.isEmpty()) {
                    try {
                        var result = domainResults.get(row.getId());

                        if (result == null) {
                            throw new IllegalStateException(
                                    "Domain validation unavailable"
                            );
                        }

                        messages.addAll(result.errors());
                        row.setAffectedResourceId(
                                result.affectedResourceId()
                        );
                        row.setCurrentValues(json(
                                result.currentValues() == null
                                        ? Map.of()
                                        : result.currentValues()
                        ));

                        if (result.duplicateKey() != null) {
                            AiImportRow other = seen.putIfAbsent(
                                    result.duplicateKey(),
                                    row
                            );

                            if (other != null) {
                                messages.add("DUPLICATE_IN_WORKBOOK");
                                error(other, "DUPLICATE_IN_WORKBOOK");
                                other.setStatus("DUPLICATE");
                            }
                        }

                        if (tool == ImportTool.PRODUCT_SKU_BULK_CREATE
                                && v.get("barcode") != null
                                && !v.get("barcode").isBlank()) {
                            AiImportRow other = seen.putIfAbsent(
                                    "BARCODE:" + v.get("barcode"),
                                    row
                            );

                            if (other != null) {
                                messages.add(
                                        "DUPLICATE_BARCODE_IN_WORKBOOK"
                                );
                                error(
                                        other,
                                        "DUPLICATE_BARCODE_IN_WORKBOOK"
                                );
                                other.setStatus("DUPLICATE");
                            }
                        }
                    } catch (Exception ex) {
                        messages.add("DOMAIN_VALIDATION_UNAVAILABLE");
                    }
                }
            }

            if (!messages.isEmpty()) {
                valid = false;
                row.setStatus(
                        messages.stream()
                                .anyMatch(m -> m.contains("DUPLICATE"))
                                ? "DUPLICATE"
                                : "INVALID"
                );
                messages.forEach(m -> error(row, m));
            }
        }

        batch.setApprovedRevision(null);
        batch.setReviewerId(null);
        batch.setValidatedRevision(valid ? batch.getRevision() : null);
        batch.setStatus(valid ? "VALIDATED" : "INVALID");

        List<AiImportValidationError> validationErrors =
                errors.findByBatchId(id);
        Set<String> codes = new TreeSet<>();
        validationErrors.forEach(e ->
                codes.add(e.getMessage().split(":", 2)[0])
        );
        Map<String, String> explanations = mappingClient.explain(codes);

        if (explanations != null) {
            validationErrors.forEach(e ->
                    e.setExplanation(
                            explanations.get(
                                    e.getMessage().split(":", 2)[0]
                            )
                    )
            );
        }

        record(batch, "VALIDATED", Map.of("valid", valid));

        if (valid) {
            AiImportRevision revision = new AiImportRevision();
            revision.setBatchId(id);
            revision.setRevision(batch.getRevision());
            revision.setSnapshot(json(Map.of(
                    "sheets", bySheet.values(),
                    "rows", allRows
            )));
            revisions.save(revision);
        }

        return valid;
    }

    private void error(AiImportRow row, String message) {
        AiImportValidationError error = new AiImportValidationError();
        error.setBatchId(row.getBatchId());
        error.setRowId(row.getId());
        error.setMessage(message);
        errors.save(error);
    }

    @Transactional
    public AiImportBatch submit(UUID id, int version, long reviewerId) {
        require("IMPORT_UPLOAD");
        AiImportBatch batch = lock(id, version);
        editable(batch);

        if (batch.getOwnerId() != CurrentActor.userId()) {
            throw new AccessDeniedException("Only the uploader can submit");
        }

        if (!auth.importReviewer(token(), reviewerId)
                || reviewerId == batch.getOwnerId()
                || Objects.equals(batch.getLastEditorId(), reviewerId)) {
            throw bad("Your Reports to manager must be an independent authorized reviewer. Ask an administrator to check your manager and their approval permissions.");
        }

        if (!validateData(batch)) {
            return batch;
        }

        batch.setAssignedReviewerId(reviewerId);
        batch.setStatus("SUBMITTED");
        batch.setSubmittedAt(Instant.now());
        record(batch, "SUBMITTED", Map.of("reviewerId", reviewerId));
        notify(batch, NotificationEvent.Type.IMPORT_SUBMITTED);
        return batch;
    }

    @Transactional
    public AiImportBatch decide(
            UUID id,
            int version,
            String decision,
            String reason
    ) {
        require("IMPORT_REVIEW");

        if (!Set.of("APPROVED", "REJECTED", "CHANGES_REQUESTED")
                .contains(decision)) {
            throw bad("Unsupported review decision");
        }

        if (reason == null
                || reason.isBlank()
                || reason.length() > 2000) {
            throw bad(
                    "A review reason of 1–2000 characters is required"
            );
        }

        AiImportBatch batch = lock(id, version);

        if (!"SUBMITTED".equals(batch.getStatus())) {
            throw conflict("Batch is not pending approval");
        }

        long actor = CurrentActor.userId();

        if (!Objects.equals(actor, batch.getAssignedReviewerId())
                || actor == batch.getOwnerId()
                || Objects.equals(batch.getLastEditorId(), actor)) {
            throw new AccessDeniedException("Assigned reviewer required");
        }

        if ("APPROVED".equals(decision)) {
            require("IMPORT_APPROVE");
            require("IMPORT_EXECUTE");

            if (actor == batch.getOwnerId()
                    || Objects.equals(
                    batch.getLastEditorId(),
                    actor
            )) {
                throw new AccessDeniedException(
                        "Independent reviewer required"
                );
            }

            if (!validateData(batch)) {
                return batch;
            }

            batch.setApprovedRevision(batch.getRevision());
            batch.setReviewerId(actor);
        }

        batch.setStatus(decision);
        batch.setReviewedAt(Instant.now());

        AiImportApproval approval = new AiImportApproval();
        approval.setBatchId(id);
        approval.setRevision(version);
        approval.setActorId(actor);
        approval.setDecision(decision);
        approval.setReason(reason);
        approvals.save(approval);

        record(batch, decision, reason);
        notify(batch, switch (decision) {
            case "APPROVED" -> NotificationEvent.Type.IMPORT_APPROVED;
            case "REJECTED" -> NotificationEvent.Type.IMPORT_REJECTED;
            default -> NotificationEvent.Type.IMPORT_CHANGES_REQUESTED;
        });
        return batch;
    }

    @Transactional
    public AiImportBatch execute(UUID id, int version) {
        require("IMPORT_EXECUTE");
        require("IMPORT_APPROVE");
        require("IMPORT_REVIEW");
        AiImportBatch batch = lock(id, version);

        if (!Objects.equals(batch.getReviewerId(), CurrentActor.userId())
                || !Objects.equals(
                batch.getApprovedRevision(),
                batch.getRevision()
        )) {
            throw new AccessDeniedException(
                    "Current approved revision and reviewer required"
            );
        }

        if ("COMPLETED".equals(batch.getStatus())) {
            return batch;
        }

        if (!Set.of(
                "APPROVED",
                "PARTIAL_SUCCESS",
                "FAILED",
                "PROCESSING"
        ).contains(batch.getStatus())) {
            throw conflict("Batch is not executable");
        }

        AiImportExecution execution = new AiImportExecution();
        execution.setBatchId(id);
        execution.setRevision(version);
        executions.save(execution);

        batch.setStatus("PROCESSING");
        notify(batch, NotificationEvent.Type.IMPORT_PROCESSING);

        Map<UUID, AiImportSheet> bySheet = new HashMap<>();
        sheets.findByBatchId(id).forEach(s -> bySheet.put(s.getId(), s));

        int processed = 0;
        List<AiImportRow> all = rows.findByBatchId(id);
        all.sort(Comparator.comparing(r -> "FAILED".equals(r.getStatus())));

        for (AiImportRow row : all) {
            if ("CREATED".equals(row.getStatus())
                    || "SKIPPED".equals(row.getStatus())) {
                continue;
            }

            if (++processed > 10) {
                break;
            }

            ImportTool tool = tool(bySheet.get(row.getSheetId()));
            require(tool.permission);

            String key = id + ":" + row.getId();
            AiImportRowExecution attempt = new AiImportRowExecution();
            attempt.setBatchId(id);
            attempt.setExecutionId(execution.getId());
            attempt.setRowId(row.getId());
            attempt.setIdempotencyKey(key);

            try {
                var data = new ImportContracts.Row(
                        tool,
                        values(row.getValuesJson())
                );

                if (!tool.validate(data.values()).isEmpty()) {
                    throw new IllegalStateException(
                            "Invalid approved data"
                    );
                }

                if (!imageErrors(batch, row, tool).isEmpty()) {
                    throw new IllegalStateException(
                            "Approved images unavailable"
                    );
                }

                String signed = proof.issue(
                        key,
                        data,
                        CurrentActor.userId(),
                        version
                );
                var result = tool == ImportTool.PRODUCT_BULK_CREATE
                        || tool == ImportTool.PRODUCT_BULK_UPDATE
                        && !data.values()
                        .getOrDefault("imageIds", "")
                        .isBlank()
                        ? domain.executeProduct(
                        token(),
                        key,
                        signed,
                        json(data),
                        imageFiles(batch, row)
                )
                        : domain.execute(token(), key, signed, data);

                if (result == null
                        || result.resourceId() == null
                        || result.resourceId().isBlank()) {
                    throw new IllegalStateException(
                            "Domain result unavailable"
                    );
                }

                row.setStatus("CREATED");
                row.setResourceId(result.resourceId());
                attempt.setResourceId(result.resourceId());
                attempt.setStatus("CREATED");
            } catch (Exception ex) {
                row.setStatus("FAILED");
                attempt.setStatus("FAILED");
                attempt.setErrorCode("DOMAIN_RESULT_UNCONFIRMED");
            }

            rowExecutions.save(attempt);
        }

        long created = all.stream()
                .filter(r -> "CREATED".equals(r.getStatus()))
                .count();
        long pending = all.stream()
                .filter(r -> !Set.of("CREATED", "FAILED", "SKIPPED")
                        .contains(r.getStatus()))
                .count();
        long skipped = all.stream().filter(AiImportRow::isSkipped).count();

        String status = pending > 0
                ? "PROCESSING"
                : created + skipped == all.size()
                ? "COMPLETED"
                : created > 0 ? "PARTIAL_SUCCESS" : "FAILED";

        batch.setStatus(status);
        execution.setStatus(status);
        execution.setCompletedAt(Instant.now());
        record(
                batch,
                status,
                Map.of("created", created, "pending", pending)
        );

        if (!status.equals("PROCESSING")) {
            notify(
                    batch,
                    NotificationEvent.Type.valueOf("IMPORT_" + status)
            );
        }

        return batch;
    }

    @Transactional
    public AiImportBatch attachImage(
            UUID id,
            UUID rowId,
            int version,
            org.springframework.web.multipart.MultipartFile file
    ) {
        AiImportBatch batch = lock(id, version);
        editable(batch);
        AiImportRow row = rows.findById(rowId)
                .filter(r -> r.getBatchId().equals(id))
                .orElseThrow(AiImportService::notFound);
        AiImportSheet sheet = sheets.findById(row.getSheetId())
                .orElseThrow(AiImportService::notFound);

        if (!sheet.isConfirmed()
                || !Set.of(
                ImportTool.PRODUCT_BULK_CREATE,
                ImportTool.PRODUCT_BULK_UPDATE
        ).contains(tool(sheet))) {
            throw bad("Confirm a Product mapping before attaching images");
        }

        require(tool(sheet).permission);
        ImportImageFile clean = ImportImageFile.sanitize(file);
        Map<String, String> data = values(row.getValuesJson());
        List<String> ids = new ArrayList<>();
        List<String> hashes = new ArrayList<>();

        if (!data.getOrDefault("imageIds", "").isBlank()) {
            ids.addAll(Arrays.asList(data.get("imageIds").split(",")));
            hashes.addAll(Arrays.asList(
                    data.getOrDefault("imageChecksums", "").split(",")
            ));
        }

        if (hashes.contains(clean.checksum())) {
            return batch;
        }

        if (ids.size() >= 4
                || images.countByBatchId(id) >= 20
                || images.bytes(id) + clean.getSize() > 52_428_800) {
            throw bad(
                    "Image limits: four per row, twenty per batch, "
                            + "fifty MiB total"
            );
        }

        AiImportImage image = new AiImportImage();
        image.setBatchId(id);
        image.setRowId(rowId);
        image.setActorId(CurrentActor.userId());
        image.setChecksum(clean.checksum());

        String name = Objects.requireNonNullElse(
                        file.getOriginalFilename(),
                        "product"
                )
                .replaceAll("[^A-Za-z0-9._-]", "_");
        image.setFilename(
                name.substring(0, Math.min(name.length(), 100)) + ".png"
        );
        image.setContent(clean.getBytes());
        image.setSizeBytes((int) clean.getSize());
        images.save(image);

        changed(batch, "PRODUCT_IMAGE_ATTACHED", Map.of(
                "rowId", rowId,
                "imageId", image.getId(),
                "checksum", image.getChecksum()
        ));
        ids.add(image.getId().toString());
        hashes.add(image.getChecksum());
        data.put("imageIds", String.join(",", ids));
        data.put("imageChecksums", String.join(",", hashes));
        row.setValuesJson(json(data));
        row.setEdited(true);
        return batch;
    }

    @Transactional
    public AiImportBatch detachImage(
            UUID id,
            UUID rowId,
            UUID imageId,
            int version
    ) {
        AiImportBatch batch = lock(id, version);
        editable(batch);
        AiImportRow row = rows.findById(rowId)
                .filter(r -> r.getBatchId().equals(id))
                .orElseThrow(AiImportService::notFound);
        require(tool(
                sheets.findById(row.getSheetId()).orElseThrow()
        ).permission);
        Map<String, String> data = values(row.getValuesJson());
        List<String> ids = new ArrayList<>(Arrays.asList(
                data.getOrDefault("imageIds", "").split(",")
        ));
        List<String> hashes = new ArrayList<>(Arrays.asList(
                data.getOrDefault("imageChecksums", "").split(",")
        ));
        int index = ids.indexOf(imageId.toString());

        if (index < 0 || index >= hashes.size()) {
            throw bad("Image is not attached to this row");
        }

        ids.remove(index);
        hashes.remove(index);
        changed(batch, "PRODUCT_IMAGE_DETACHED", Map.of(
                "rowId", rowId,
                "imageId", imageId
        ));
        data.put("imageIds", String.join(",", ids));
        data.put("imageChecksums", String.join(",", hashes));
        row.setValuesJson(json(data));
        row.setEdited(true);
        return batch;
    }

    @Transactional(readOnly = true)
    public AiImportImage image(UUID id, UUID imageId) {
        read(id);
        return images.findById(imageId)
                .filter(i -> i.getBatchId().equals(id))
                .orElseThrow(AiImportService::notFound);
    }

    private List<String> imageErrors(
            AiImportBatch batch,
            AiImportRow row,
            ImportTool tool
    ) {
        if (tool != ImportTool.PRODUCT_BULK_CREATE
                && (tool != ImportTool.PRODUCT_BULK_UPDATE
                || values(row.getValuesJson())
                .getOrDefault("imageIds", "")
                .isBlank())) {
            return List.of();
        }

        try {
            imageFiles(batch, row);
            return List.of();
        } catch (Exception ex) {
            return List.of("INVALID_IMAGE_REFERENCES");
        }
    }

    private List<org.springframework.web.multipart.MultipartFile> imageFiles(
            AiImportBatch batch,
            AiImportRow row
    ) {
        Map<String, String> values = values(row.getValuesJson());
        String[] ids = values.getOrDefault("imageIds", "").split(",");
        String[] hashes = values.getOrDefault("imageChecksums", "")
                .split(",");

        if (ids.length < 1
                || ids.length > 4
                || ids.length != hashes.length
                || new HashSet<>(Arrays.asList(ids)).size()
                != ids.length) {
            throw bad("Product images required");
        }

        List<org.springframework.web.multipart.MultipartFile> result =
                new ArrayList<>();

        for (int i = 0; i < ids.length; i++) {
            AiImportImage image = images.findById(
                            UUID.fromString(ids[i])
                    )
                    .filter(v ->
                            v.getBatchId().equals(batch.getId())
                                    && v.getRowId().equals(row.getId())
                    )
                    .orElseThrow(() -> bad(
                            "Image belongs to another row or batch"
                    ));
            ImportImageFile file = new ImportImageFile(
                    image.getFilename(),
                    image.getContent()
            );

            if (!file.checksum().equals(image.getChecksum())
                    || !image.getChecksum().equals(hashes[i])) {
                throw bad("Image checksum mismatch");
            }

            result.add(file);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> executions(UUID id) {
        read(id);
        return Map.of(
                "executions", executions.findByBatchId(id),
                "rows", rowExecutions.findByBatchId(id)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(UUID id) {
        read(id);
        return Map.of(
                "audit", history.findByBatchId(id),
                "approvals", approvals.findByBatchId(id),
                "revisions", revisions.findByBatchId(id)
        );
    }

    @Transactional(readOnly = true)
    public byte[] original(UUID id) {
        read(id);
        return files.findByBatchId(id).get(0).getContent();
    }

    @Transactional(readOnly = true)
    public String report(UUID id) {
        read(id);
        StringBuilder csv = new StringBuilder(
                "sheet,row,status,original,edited,resourceId,errors\r\n"
        );
        Map<UUID, String> names = new HashMap<>();
        sheets.findByBatchId(id).forEach(s ->
                names.put(s.getId(), s.getName())
        );
        Map<UUID, List<String>> messages = new HashMap<>();
        errors.findByBatchId(id).forEach(e ->
                messages.computeIfAbsent(
                        e.getRowId(),
                        k -> new ArrayList<>()
                ).add(e.getMessage())
        );
        rowExecutions.findByBatchId(id).stream()
                .filter(e -> e.getErrorCode() != null)
                .forEach(e ->
                        messages.computeIfAbsent(
                                e.getRowId(),
                                k -> new ArrayList<>()
                        ).add(e.getErrorCode())
                );

        for (AiImportRow row : rows.findByBatchId(id)) {
            csv.append(
                    String.join(
                            ",",
                            List.of(
                                    csv(names.get(row.getSheetId())),
                                    csv(Integer.toString(
                                            row.getRowNumber()
                                    )),
                                    csv(row.getStatus()),
                                    csv(row.getOriginalValues()),
                                    csv(row.getValuesJson()),
                                    csv(row.getResourceId()),
                                    csv(String.join(
                                            "; ",
                                            messages.getOrDefault(
                                                    row.getId(),
                                                    List.of()
                                            )
                                    ))
                            )
                    )
            ).append("\r\n");
        }

        recordRead(id, "REPORT_DOWNLOADED");
        return csv.toString();
    }

    public static String csv(String value) {
        String text = Objects.requireNonNullElse(value, "");
        String stripped = text.stripLeading();

        if (!stripped.isEmpty()
                && "=+-@".indexOf(stripped.charAt(0)) >= 0
                || text.startsWith("\t")
                || text.startsWith("\r")
                || text.startsWith("\n")) {
            text = "'" + text;
        }

        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private AiImportColumnMapping mapping(UUID id, UUID sheet) {
        return mappings.findByBatchId(id).stream()
                .filter(m -> m.getSheetId().equals(sheet))
                .findFirst()
                .orElseThrow();
    }

    private ImportTool tool(AiImportSheet sheet) {
        if (sheet.getTool() == null) {
            throw bad("Confirm the sheet type first");
        }

        return ImportTool.valueOf(sheet.getTool());
    }

    private Map<String, String> values(String json) {
        try {
            return mapper.readValue(
                    json,
                    new TypeReference<LinkedHashMap<String, String>>() {}
            );
        } catch (Exception ex) {
            throw bad("Invalid row data");
        }
    }

    private List<String> parseHeaders(String json) {
        try {
            return mapper.readValue(
                    json,
                    new TypeReference<List<String>>() {}
            );
        } catch (Exception ex) {
            throw bad("Invalid headers");
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot serialize import data",
                    ex
            );
        }
    }

    private void recordRead(UUID id, String action) {
        audit.record(
                AuditEvent.success(
                        AuditAction.AI_TOOL_EXECUTION,
                        "AI_IMPORT",
                        id,
                        null,
                        Map.of("action", action)
                )
        );
    }

    private void record(
            AiImportBatch batch,
            String action,
            Object details
    ) {
        AiImportAuditHistory event = new AiImportAuditHistory();
        event.setBatchId(batch.getId());
        event.setRevision(batch.getRevision());
        event.setActorId(CurrentActor.userId());
        event.setAction(action);
        event.setDetails(details == null ? null : json(details));
        event.setRequestId(RequestIds.current());
        event.setTraceId(MDC.get("traceId"));
        history.save(event);
        recordRead(batch.getId(), action);
    }

    private void notify(
            AiImportBatch batch,
            NotificationEvent.Type type
    ) {
        Set<Long> recipients = new HashSet<>();
        recipients.add(batch.getOwnerId());

        if (batch.getAssignedReviewerId() != null) {
            recipients.add(batch.getAssignedReviewerId());
        }

        for (long recipient : recipients) {
            UUID id = UUID.randomUUID();
            var event = new NotificationEvent(
                    1,
                    id,
                    RequestIds.current(),
                    MDC.get("traceId"),
                    MDC.get("spanId"),
                    null,
                    recipient,
                    type,
                    "AI_IMPORT",
                    batch.getId().toString(),
                    Instant.now()
            );
            event.validate();

            NotificationOutbox row = new NotificationOutbox();
            row.setId(id);
            row.setPayload(json(event));
            row.setCreatedAt(Instant.now());
            row.setNextAttemptAt(Instant.now());
            outbox.save(row);
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Import not found"
        );
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
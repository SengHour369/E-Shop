package com.example.eshop.ai.controller;

import com.example.eshop.ai.service.*;
import com.example.eshop.common.imports.ImportTool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;

@RestController @RequestMapping("/api/ai/imports") @RequiredArgsConstructor @Validated
public class AiImportController {
    private final AiImportService service;
    private final ExcelImportParser parser;
    public record Version(@Min(1) int version) {}
    public record Submit(@Min(1) int version, @Positive long reviewerId) {}
    public record Decision(@Min(1) int version, @NotBlank @Size(max = 2000) String reason) {}
    public record Edit(@Min(1) int version, @NotNull @Size(max = 64) Map<String, String> values) {}
    public record Mapping(@Min(1) int version, @NotNull ImportTool tool, @NotNull @Size(max = 64) Map<String, String> columns) {}
    public record Skip(@Min(1) int version, boolean skipped) {}
    public record AddRow(@Min(1) int version, @NotNull UUID rowId, @NotNull @Size(max = 64) Map<String, String> values) {}
    @GetMapping("/tools") public Object tools() { return service.registry(); }
    @GetMapping public Object list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size, @RequestParam(defaultValue = "false") boolean review,
            @RequestParam(defaultValue = "false") boolean assigned) { return service.list(page, size, review, assigned); }
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object upload(@RequestPart("file") MultipartFile file) { service.require("IMPORT_UPLOAD"); return service.upload(parser.parse(file)); }
    @PostMapping(value="/{id}/rows/{rowId}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object attachImage(@PathVariable UUID id,@PathVariable UUID rowId,@RequestParam @Min(1) int version,@RequestPart("file") MultipartFile file){return service.attachImage(id,rowId,version,file);}
    @PostMapping("/{id}/rows/{rowId}/images/{imageId}/detach")
    public Object detachImage(@PathVariable UUID id,@PathVariable UUID rowId,@PathVariable UUID imageId,@Valid @RequestBody Version version){return service.detachImage(id,rowId,imageId,version.version());}
    @GetMapping("/{id}/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable UUID id,@PathVariable UUID imageId){var image=service.image(id,imageId);return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").header("Cache-Control","no-store").body(image.getContent());}
    @PostMapping("/{id}/analyze") public Object analyze(@PathVariable UUID id, @Valid @RequestBody Version request) { return service.analyze(id, request.version()); }
    @GetMapping("/{id}/preview") public Object preview(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Pattern(regexp = "VALID|INVALID|DUPLICATE|EDITED|CREATED|FAILED|SKIPPED|UNVALIDATED") String status) { return service.preview(id, page, size, status); }
    @PatchMapping("/{id}/sheets/{sheetId}") public Object mapping(@PathVariable UUID id, @PathVariable UUID sheetId, @Valid @RequestBody Mapping request) { return service.confirmMapping(id, sheetId, request.version(), request.tool(), request.columns()); }
    @PostMapping("/{id}/sheets/{sheetId}/rows")
    public Object addRow(@PathVariable UUID id, @PathVariable UUID sheetId, @Valid @RequestBody AddRow request) {
        return service.addRow(id, sheetId, request.version(), request.rowId(), request.values());
    }
    @PatchMapping("/{id}/rows/{rowId}") public Object edit(@PathVariable UUID id, @PathVariable UUID rowId, @Valid @RequestBody Edit request) { return service.edit(id, rowId, request.version(), request.values()); }
    @PatchMapping("/{id}/rows/{rowId}/skip") public Object skip(@PathVariable UUID id, @PathVariable UUID rowId, @Valid @RequestBody Skip request) { return service.skip(id, rowId, request.version(), request.skipped()); }
    @PostMapping("/{id}/validate") public Object validate(@PathVariable UUID id, @Valid @RequestBody Version request) { return service.validate(id, request.version()); }
    @PostMapping("/{id}/submit") public Object submit(@PathVariable UUID id, @Valid @RequestBody Submit request) { return service.submit(id, request.version(), request.reviewerId()); }
    @PostMapping("/{id}/approve") public Object approve(@PathVariable UUID id, @Valid @RequestBody Decision request) { return service.decide(id, request.version(), "APPROVED", request.reason()); }
    @PostMapping("/{id}/reject") public Object reject(@PathVariable UUID id, @Valid @RequestBody Decision request) { return service.decide(id, request.version(), "REJECTED", request.reason()); }
    @PostMapping("/{id}/request-changes") public Object changes(@PathVariable UUID id, @Valid @RequestBody Decision request) { return service.decide(id, request.version(), "CHANGES_REQUESTED", request.reason()); }
    @PostMapping("/{id}/execute") public Object execute(@PathVariable UUID id, @Valid @RequestBody Version request) { return service.execute(id, request.version()); }
    @GetMapping("/{id}/executions") public Object executions(@PathVariable UUID id) { return service.executions(id); }
    @GetMapping("/{id}/history") public Object history(@PathVariable UUID id) { return service.history(id); }
    @GetMapping("/{id}/original") public ResponseEntity<byte[]> original(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=original-workbook.bin").body(service.original(id));
    }
    @GetMapping(value = "/{id}/report", produces = "text/csv") public ResponseEntity<String> report(@PathVariable UUID id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import-report.csv").body(service.report(id));
    }
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<Map<String, String>> duplicate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "IMPORT_CONFLICT", "message", "This workbook has already been staged or the import changed concurrently."));
    }
}

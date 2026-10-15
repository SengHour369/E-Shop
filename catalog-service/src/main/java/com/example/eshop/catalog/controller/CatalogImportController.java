package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.service.CatalogImportService;
import com.example.eshop.common.imports.ImportContracts.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ai/imports")
@RequiredArgsConstructor
public class CatalogImportController {

    private final CatalogImportService service;

    @PostMapping("/validate")
    public Validation validate(@RequestBody Row row) {
        return service.validate(row);
    }

    @PostMapping("/validate-bulk")
    public java.util.List<Validation> validateBulk(
            @RequestBody java.util.List<Row> rows
    ) {
        return service.validateBulk(rows);
    }

    @PostMapping(value = "/execute-product", consumes = "multipart/form-data")
    public Result product(
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader("X-Import-Approval") String approval,
            @RequestPart("row") String json,
            @RequestPart("files")
            java.util.List<org.springframework.web.multipart.MultipartFile> files
    ) throws Exception {
        if (json.length() > 16384) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid row"
            );
        }

        Row row = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, Row.class);

        if (row.tool() != com.example.eshop.common.imports.ImportTool.PRODUCT_BULK_CREATE) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Product tool required"
            );
        }

        return service.execute(key, approval, row, files);
    }

    @PostMapping("/execute")
    public Result execute(
            @RequestHeader("Idempotency-Key") String key,
            @RequestHeader("X-Import-Approval") String approval,
            @RequestBody Row row
    ) throws Exception {
        return service.execute(key, approval, row);
    }
}
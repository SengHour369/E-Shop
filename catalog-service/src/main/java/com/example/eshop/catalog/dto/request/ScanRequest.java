package com.example.eshop.catalog.dto.request;

import com.example.eshop.catalog.scanner.ScanFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The camera client sends a decoded value. Price, stock, and product ids in this body are ignored. */
public record ScanRequest(
        @NotBlank(message = "Scanner code is required")
        @Size(max = 512, message = "Scanner code is too long")
        String code,
        ScanFormat format
) {}

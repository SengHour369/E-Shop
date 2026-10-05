package com.example.eshop.catalog.controller;

import com.example.eshop.catalog.dto.request.ScanRequest;
import com.example.eshop.catalog.dto.response.ImageMatchResponse;
import com.example.eshop.catalog.dto.response.ScanResponse;
import com.example.eshop.catalog.scanner.ScanRateLimitedException;
import com.example.eshop.catalog.service.impl.ProductScannerService;
import com.example.eshop.common.dto.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/product-scanner")
@RequiredArgsConstructor
public class ProductScannerController {
    private final ProductScannerService scanner;

    @PostMapping("/scan")
    public ResponseEntity<APIResponse<ScanResponse>> scan(@Valid @RequestBody ScanRequest request) {
        ScanResponse body = scanner.scan(request);
        String message = body.isFound() ? body.getMessage() : "Product not found";
        return ResponseEntity.ok(APIResponse.success(message, 200, body));
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<ImageMatchResponse>> recognize(@RequestParam("image") MultipartFile image) {
        ImageMatchResponse body = scanner.recognize(image);
        return ResponseEntity.ok(APIResponse.success(body.getMessage(), 200, body));
    }

    @ExceptionHandler(ScanRateLimitedException.class)
    public ResponseEntity<APIResponse<Object>> limited() {
        return ResponseEntity.status(429)
                .header("Retry-After", "1")
                .body(APIResponse.error("Too many scan requests", 429));
    }
}

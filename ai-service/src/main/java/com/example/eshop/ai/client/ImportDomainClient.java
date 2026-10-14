package com.example.eshop.ai.client;
import com.example.eshop.common.imports.ImportContracts.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "catalog-service", contextId = "importDomain")
public interface ImportDomainClient {
    @PostMapping("/internal/ai/imports/validate")
    Validation validate(@RequestHeader("Authorization") String token, @RequestBody Row row);
    @PostMapping("/internal/ai/imports/validate-bulk")
    java.util.List<Validation> validateBulk(@RequestHeader("Authorization") String token, @RequestBody java.util.List<Row> rows);
    @PostMapping(value="/internal/ai/imports/execute-product",consumes="multipart/form-data")
    Result executeProduct(@RequestHeader("Authorization") String token,@RequestHeader("Idempotency-Key") String key,
            @RequestHeader("X-Import-Approval") String proof,@RequestPart("row") String row,@RequestPart("files") java.util.List<org.springframework.web.multipart.MultipartFile> files);
    @PostMapping("/internal/ai/imports/execute")
    Result execute(@RequestHeader("Authorization") String token, @RequestHeader("Idempotency-Key") String key,
            @RequestHeader("X-Import-Approval") String proof, @RequestBody Row row);
}

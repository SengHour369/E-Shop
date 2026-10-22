package com.example.eshop.common.imports;

import java.util.List;
import java.util.Map;

public final class ImportContracts {
    private ImportContracts() {}
    public record Row(ImportTool tool, Map<String, String> values) {}
    public record Validation(List<String> errors, String duplicateKey, String affectedResourceId, Map<String,String> currentValues) {
        public Validation(List<String> errors,String duplicateKey,String affectedResourceId) { this(errors,duplicateKey,affectedResourceId,Map.of()); }
    }
    public record Result(String resourceId) {}
}

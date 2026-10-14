package com.example.eshop.common.imports;

import java.util.List;
import java.util.Map;

public final class ImportContracts {
    private ImportContracts() {}
    public record Row(ImportTool tool, Map<String, String> values) {}
    public record Validation(List<String> errors, String duplicateKey, String affectedResourceId) {}
    public record Result(String resourceId) {}
}

package com.example.eshop.ai.service;

import java.util.List;
import java.util.Map;

/** Source data for staged manual and assistant approval requests. */
public final class ApprovalStagingData {
    private ApprovalStagingData() {}
    public record ParsedRow(int number, Map<String, String> values) {}
    public record ParsedSheet(String name, List<String> headers, List<ParsedRow> rows) {}
    public record ParsedFile(String filename, byte[] bytes, List<ParsedSheet> sheets) {}
}

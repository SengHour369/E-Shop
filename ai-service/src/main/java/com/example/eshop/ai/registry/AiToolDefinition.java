package com.example.eshop.ai.registry;
import com.example.eshop.ai.enums.*;
import java.util.Map;
public record AiToolDefinition(AiIntent toolName, String description, String service, String operation,
    String method, String path, Map<String, Parameter> requestSchema, String requiredPermission,
    AiToolRisk risk, boolean notifyOutcome, boolean enabled) {
    public record Parameter(String type, boolean required, String description) {}
}

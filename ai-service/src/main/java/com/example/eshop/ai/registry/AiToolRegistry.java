package com.example.eshop.ai.registry;
import com.example.eshop.ai.enums.*;
import com.example.eshop.ai.service.AiFailure;
import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import java.util.*;
import static com.example.eshop.ai.enums.AiIntent.*;
import static com.example.eshop.ai.enums.AiToolRisk.*;
import static com.example.eshop.ai.enums.AiExecutionStatus.*;
import static com.example.eshop.ai.registry.AiToolDefinition.*;
@Component
public class AiToolRegistry {
    private final Map<AiIntent, AiToolDefinition> tools = new EnumMap<>(AiIntent.class);
    public AiToolRegistry() {
        add(PRODUCT_GET, "Get an active product", "catalog-service", "GET", "/internal/ai/products/{id}", "AUTHENTICATED", READ_ONLY, false, true, Map.of("id", p("integer", true)));
        add(PRODUCT_SEARCH, "Search products; one page", "catalog-service", "POST", "/api/v1/products/get/all", "AUTHENTICATED", READ_ONLY, false, true, Map.of("query", p("string", true)));
        add(SKU_GET, "Get an active product SKU", "catalog-service", "GET", "/internal/ai/skus/{skuId}", "AUTHENTICATED", READ_ONLY, false, true, Map.of("skuId", p("integer", true)));
        add(INVENTORY_GET, "Get inventory for SKU", "catalog-service", "POST", "/api/v1/inventory/sku/", "ADMIN", READ_ONLY, false, true, Map.of("skuId", p("integer", true)));
        add(INVENTORY_LOW_STOCK, "Find low stock; default threshold 10, first 20 rows", "catalog-service", "POST", "/api/v1/inventory/low-stock", "ADMIN", READ_ONLY, false, true, Map.of("threshold", p("integer", false)));
        add(ORDER_GET, "Get your own order by order number", "order-service", "GET", "/internal/ai/orders/{orderNumber}", "AUTHENTICATED", READ_ONLY, false, true, Map.of("orderNumber", p("string", true)));
        add(PROMOTION_GET, "Get promotion details", "catalog-service", "GET", "/api/v1/admin/promotions/{id}", "ADMIN", READ_ONLY, false, true, Map.of("id", p("integer", true)));
        add(PROMOTION_CREATE, "Create a DRAFT percentage promotion for one SKU; never activate. Require explicit name and start/end local dates (ISO 8601); do not invent missing values.",
            "catalog-service", "POST", "/internal/ai/promotions", "ADMIN", LOW_RISK_WRITE, true, true,
            Map.of("skuId", p("integer", true), "name", p("string", true), "discount", p("number", true), "startAt", p("string", true), "endAt", p("string", true)));
        for (AiIntent intent : List.of(ORDER_CANCEL, PAYMENT_REFUND, ROLE_GRANT))
            add(intent, "Unavailable sensitive operation", "disabled", "NONE", "", "ADMIN", HIGH_RISK_WRITE, false, false, Map.of());
    }
    private static Parameter p(String type, boolean required) { return new Parameter(type, required, type); }
    private void add(AiIntent name, String description, String service, String method, String path, String permission,
                     AiToolRisk risk, boolean notify, boolean enabled, Map<String, Parameter> schema) {
        tools.put(name, new AiToolDefinition(name, description, service, name.name(), method, path, schema, permission, risk, notify, enabled));
    }
    public AiToolDefinition find(AiIntent intent) { return intent == null ? null : tools.get(intent); }
    public boolean permitted(AiToolDefinition tool) {
        return tool != null && ("AUTHENTICATED".equals(tool.requiredPermission()) || CurrentActor.has(tool.requiredPermission()));
    }
    public List<AiToolDefinition> available() {
        CurrentActor.userId();
        return tools.values().stream().filter(AiToolDefinition::enabled).filter(this::permitted).toList();
    }
    public AiToolDefinition authorize(AiIntent intent) {
        var tool = find(intent);
        if (tool == null) throw new AiFailure("UNKNOWN_INTENT", NEEDS_INPUT, "Please specify a supported operation.");
        if (!tool.enabled()) throw new AiFailure("TOOL_DISABLED", DENIED, "This operation is unavailable through AI.");
        if (!permitted(tool)) throw new AiFailure("PERMISSION_DENIED", DENIED, "You cannot perform this operation.");
        if (tool.risk() == HIGH_RISK_WRITE) throw new AiFailure("CONFIRMATION_REQUIRED", DENIED, "Use the dedicated workflow for this sensitive operation.");
        return tool;
    }
    public void validate(AiToolDefinition tool, JsonNode params) {
        if (params == null || !params.isObject()) invalid("Invalid parameters.");
        var names = params.fieldNames();
        while (names.hasNext()) if (!tool.requestSchema().containsKey(names.next())) invalid("Unexpected parameter.");
        tool.requestSchema().forEach((key, spec) -> {
            JsonNode v = params.get(key);
            if (v == null || v.isNull()) {
                if (spec.required()) throw new AiFailure("MISSING_PARAMETER", NEEDS_INPUT, "Please provide " + key + ".");
                return;
            }
            switch (spec.type()) {
                case "integer" -> { if (!v.isIntegralNumber() || !v.canConvertToLong() || v.longValue() < ("threshold".equals(key) ? 0 : 1) || ("threshold".equals(key) && v.longValue() > 1000000)) invalid("Invalid " + key + "."); }
                case "number" -> { if (!v.isNumber() || v.decimalValue().signum() <= 0 || v.decimalValue().compareTo(new java.math.BigDecimal("100")) > 0 || v.decimalValue().scale() > 4) invalid("Discount must be greater than zero and at most 100."); }
                default -> { if (!v.isTextual() || v.textValue().isBlank() || v.textValue().length() > 120 || v.textValue().chars().anyMatch(Character::isISOControl)) invalid("Invalid " + key + "."); }
            }
        });
        if (tool.toolName() == ORDER_GET && !params.path("orderNumber").asText().matches("[A-Za-z0-9_-]{1,80}")) invalid("Invalid order number.");
        if (tool.toolName() == PROMOTION_CREATE) {
            try {
                var start = java.time.LocalDateTime.parse(params.path("startAt").asText());
                var end = java.time.LocalDateTime.parse(params.path("endAt").asText());
                if (!start.isBefore(end)) invalid("startAt must be before endAt.");
            } catch (java.time.format.DateTimeParseException e) { invalid("Use ISO local dates for startAt and endAt."); }
        }
    }
    private static void invalid(String message) { throw new AiFailure("INVALID_PARAMETERS", NEEDS_INPUT, message); }
}

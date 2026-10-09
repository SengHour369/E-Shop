package com.example.eshop.ai.registry;

import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.enums.AiToolRisk;
import com.example.eshop.ai.registry.AiToolDefinition.Parameter;
import com.example.eshop.ai.service.AiFailure;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.common.security.PermissionSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.example.eshop.ai.enums.AiIntent.INVENTORY_GET;
import static com.example.eshop.ai.enums.AiIntent.INVENTORY_LOW_STOCK;
import static com.example.eshop.ai.enums.AiIntent.ORDER_CANCEL;
import static com.example.eshop.ai.enums.AiIntent.ORDER_GET;
import static com.example.eshop.ai.enums.AiIntent.PAYMENT_REFUND;
import static com.example.eshop.ai.enums.AiIntent.PRODUCT_GET;
import static com.example.eshop.ai.enums.AiIntent.PRODUCT_SEARCH;
import static com.example.eshop.ai.enums.AiIntent.PROMOTION_CREATE;
import static com.example.eshop.ai.enums.AiIntent.PROMOTION_GET;
import static com.example.eshop.ai.enums.AiIntent.ROLE_GRANT;
import static com.example.eshop.ai.enums.AiIntent.SKU_GET;
import static com.example.eshop.ai.enums.AiToolRisk.HIGH_RISK_WRITE;
import static com.example.eshop.ai.enums.AiToolRisk.LOW_RISK_WRITE;
import static com.example.eshop.ai.enums.AiToolRisk.READ_ONLY;

/**
 * The only catalog of tools. A caller sees the enabled tools they are allowed to use.
 * Authorization and parameter checks run again after inference because provider output is untrusted.
 */
@Component
public class AiToolRegistry {

    private static final String AUTHENTICATED = "AUTHENTICATED";
    private static final String PUBLIC = "PUBLIC";
    private static final String ADMIN = "PROMOTION_MANAGE";
    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");
    private static final int MAX_TEXT_LENGTH = 120;
    private static final long MAX_THRESHOLD = 1_000_000L;

    private final Map<AiIntent, AiToolDefinition> tools = new EnumMap<>(AiIntent.class);
    private final LivePermissionService permissions;

    public AiToolRegistry(LivePermissionService permissions) {
        this.permissions = permissions;
        read(AiIntent.KNOWLEDGE_SEARCH,
                "Retrieve approved policy, FAQ or help text. Never use for live price, stock, order or payment status.",
                "ai-service", "knowledge", PUBLIC, Map.of("query", parameter("string", true)));
        register(PRODUCT_GET, "Get an active product", "catalog-service", "GET",
                "/internal/ai/products/{id}", PUBLIC, READ_ONLY, false, true,
                Map.of("id", parameter("integer", true)));
        register(PRODUCT_SEARCH, "Search products; one page", "catalog-service", "GET",
                "/internal/ai/products", PUBLIC, READ_ONLY, false, true,
                Map.of("query", parameter("string", true)));
        register(SKU_GET, "Get an active product SKU", "catalog-service", "GET",
                "/internal/ai/skus/{skuId}", PUBLIC, READ_ONLY, false, true,
                Map.of("skuId", parameter("integer", true)));
        register(INVENTORY_GET, "Get inventory for SKU", "catalog-service", "GET",
                "/internal/ai/inventory/{skuId}", "INVENTORY_VIEW", READ_ONLY, false, true,
                Map.of("skuId", parameter("integer", true)));
        register(INVENTORY_LOW_STOCK, "Find low stock; default threshold 10, first 20 rows", "catalog-service", "GET",
                "/internal/ai/inventory/low-stock", "INVENTORY_VIEW", READ_ONLY, false, true,
                Map.of("threshold", parameter("integer", false)));
        register(ORDER_GET, "Get your own order by order number", "order-service", "GET",
                "/internal/ai/orders/{orderNumber}", AUTHENTICATED, READ_ONLY, false, true,
                Map.of("orderNumber", parameter("string", true)));
        register(PROMOTION_GET, "Get promotion details", "catalog-service", "GET",
                "/internal/ai/promotions/{id}", "PROMOTION_VIEW", READ_ONLY, false, true,
                Map.of("id", parameter("integer", true)));
        register(PROMOTION_CREATE,
                "Create a DRAFT percentage promotion for one SKU; never activate. Require explicit name and start/end local dates (ISO 8601); do not invent missing values.",
                "catalog-service", "POST", "/internal/ai/promotions", ADMIN, LOW_RISK_WRITE, true, true,
                Map.of(
                        "skuId", parameter("integer", true),
                        "name", parameter("string", true),
                        "discount", parameter("number", true),
                        "startAt", parameter("string", true),
                        "endAt", parameter("string", true)));
        for (AiIntent intent : List.of(PAYMENT_REFUND, ROLE_GRANT)) {
            register(intent, "Unavailable sensitive operation", "disabled", "NONE",
                    "", ADMIN, HIGH_RISK_WRITE, false, false, Map.of());
        }
        register(ORDER_CANCEL, "Cancel my pending order by explicit order number; requires confirmation",
                "order-service", "POST", "/internal/ai/orders/{number}/cancel", AUTHENTICATED,
                HIGH_RISK_WRITE, true, true, Map.of("orderNumber", parameter("string", true)));
        register(AiIntent.PROMOTION_DISABLE, "Disable a promotion by explicit ID; requires confirmation",
                "catalog-service", "POST", "/internal/ai/promotions/{id}/disable", "PROMOTION_MANAGE",
                HIGH_RISK_WRITE, true, true, Map.of("id", parameter("integer", true)));
        read(AiIntent.MY_ORDERS, "List my latest 20 orders", "order-service",
                "/internal/ai/orders", AUTHENTICATED, Map.of());
        read(AiIntent.MY_ORDER_STATUS, "Get my latest order status", "order-service",
                "/internal/ai/orders/latest", AUTHENTICATED, Map.of());
        read(AiIntent.MY_PAYMENT_STATUS, "Get my payment status by payment ID", "payment-service",
                "/internal/ai/payments/{id}", AUTHENTICATED, Map.of("id", parameter("integer", true)));
        read(AiIntent.MY_RETURNS, "List my latest 20 returns", "order-service",
                "/internal/ai/returns", AUTHENTICATED, Map.of());
        read(AiIntent.MY_NOTIFICATIONS, "List my latest 20 notifications", "notification-service",
                "/api/notifications", AUTHENTICATED, Map.of());
        read(AiIntent.ADMIN_ORDER_LIST, "List latest 20 orders by optional exact status and date (YYYY-MM-DD or TODAY)", "order-service",
                "/internal/ai/orders/admin", "ORDER_VIEW_ALL", Map.of("status", parameter("string", false), "date", parameter("string", false)));
        read(AiIntent.ADMIN_PAYMENT_LIST, "List latest 20 payments by optional exact status and date (YYYY-MM-DD or TODAY)", "payment-service",
                "/internal/ai/payments/admin", "PAYMENT_VIEW_ALL", Map.of("status", parameter("string", false), "date", parameter("string", false)));
        read(AiIntent.ADMIN_RETURN_LIST, "List latest 20 returns, optionally by exact status", "order-service",
                "/internal/ai/returns/admin", "RETURN_VIEW_ALL", Map.of("status", parameter("string", false)));
        read(AiIntent.ADMIN_ORDER_SUMMARY, "Count all orders by status", "order-service",
                "/internal/ai/orders/summary", "REPORT_VIEW", Map.of());
        read(AiIntent.ADMIN_USER_LIST, "List latest 20 accounts without credentials or personal contact details",
                "auth-service", "/internal/ai/users", "USER_VIEW", Map.of());
        read(AiIntent.ADMIN_REVENUE_SUMMARY, "Sum completed payments by currency for an explicit date (YYYY-MM-DD or TODAY); not net revenue",
                "payment-service", "/internal/ai/payments/revenue", "REPORT_VIEW",
                Map.of("date", parameter("string", true)));
        read(AiIntent.ADMIN_AUDIT_LOG, "Find latest 20 AI-service audit records by explicit requestId; excludes raw snapshots",
                "ai-service", "audit", "AUDIT_VIEW", Map.of("requestId", parameter("string", true)));
    }

    private void read(AiIntent intent, String description, String service, String path,
            String permission, Map<String, Parameter> parameters) {
        register(intent, description, service, "GET", path, permission, READ_ONLY, false, true, parameters);
    }

    public AiToolDefinition find(AiIntent intent) {
        return intent == null ? null : tools.get(intent);
    }

    public boolean permitted(AiToolDefinition tool) {
        return permitted(tool, authenticated() ? permissions.current() : null);
    }

    private boolean permitted(AiToolDefinition tool, PermissionSnapshot snapshot) {
        return tool != null && (PUBLIC.equals(tool.requiredPermission())
                || snapshot != null && (AUTHENTICATED.equals(tool.requiredPermission())
                || snapshot.administrator() && snapshot.permits(tool.requiredPermission())));
    }

    public List<AiToolDefinition> available() {
        PermissionSnapshot snapshot = authenticated() ? permissions.current() : null;
        return tools.values().stream()
                .filter(AiToolDefinition::enabled)
                .filter(tool -> permitted(tool, snapshot))
                .toList();
    }

    public static boolean authenticated() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    public AiToolDefinition authorize(AiIntent intent) {
        AiToolDefinition tool = find(intent);
        if (tool == null) {
            throw new AiFailure("UNKNOWN_INTENT", AiExecutionStatus.NEEDS_INPUT, "Please specify a supported operation.");
        }
        if (!tool.enabled()) {
            throw new AiFailure("TOOL_DISABLED", AiExecutionStatus.DENIED, "This operation is unavailable through AI.");
        }
        if (!permitted(tool)) {
            throw new AiFailure("PERMISSION_DENIED", AiExecutionStatus.DENIED, "You cannot perform this operation.");
        }
        return tool;
    }

    public void validate(AiToolDefinition tool, JsonNode params) {
        if (params == null || !params.isObject()) {
            throw invalid("Invalid parameters.");
        }
        rejectUnknownKeys(tool, params);
        tool.requestSchema().forEach((key, spec) -> checkParameter(key, spec, params.get(key)));
        if (params.hasNonNull("date") && !"TODAY".equals(params.path("date").asText())) {
            try {
                java.time.LocalDate.parse(params.path("date").asText());
            } catch (DateTimeParseException exception) {
                throw invalid("Use YYYY-MM-DD or TODAY for the date.");
            }
        }
        if (tool.toolName() == ORDER_GET || tool.toolName() == ORDER_CANCEL) {
            requireOrderNumber(params.path("orderNumber").asText());
        }
        if (tool.toolName() == PROMOTION_CREATE) {
            requirePromotionWindow(params);
        }
    }

    private static Parameter parameter(String type, boolean required) {
        return new Parameter(type, required, type);
    }

    private void register(AiIntent name, String description, String service, String method, String path,
            String permission, AiToolRisk risk, boolean notify, boolean enabled, Map<String, Parameter> schema) {
        tools.put(name, new AiToolDefinition(
                name, description, service, name.name(), method, path, schema, permission, risk, notify, enabled));
    }

    private static void rejectUnknownKeys(AiToolDefinition tool, JsonNode params) {
        Iterator<String> names = params.fieldNames();
        while (names.hasNext()) {
            if (!tool.requestSchema().containsKey(names.next())) {
                throw invalid("Unexpected parameter.");
            }
        }
    }

    private static void checkParameter(String key, Parameter spec, JsonNode value) {
        if (value == null || value.isNull()) {
            if (spec.required()) {
                throw new AiFailure("MISSING_PARAMETER", AiExecutionStatus.NEEDS_INPUT, "Please provide " + key + ".");
            }
            return;
        }
        switch (spec.type()) {
            case "integer" -> requireInteger(key, value);
            case "number" -> requirePercentage(value);
            default -> requireText(key, value);
        }
    }

    private static void requireInteger(String key, JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToLong()) {
            throw invalid("Invalid " + key + ".");
        }
        long number = value.longValue();
        long minimum = "threshold".equals(key) ? 0 : 1;
        boolean aboveMaximum = "threshold".equals(key) && number > MAX_THRESHOLD;
        if (number < minimum || aboveMaximum) {
            throw invalid("Invalid " + key + ".");
        }
    }

    private static void requirePercentage(JsonNode value) {
        if (!value.isNumber()) {
            throw invalid("Discount must be greater than zero and at most 100.");
        }
        BigDecimal amount = value.decimalValue();
        if (amount.signum() <= 0 || amount.compareTo(MAX_PERCENT) > 0 || amount.scale() > 4) {
            throw invalid("Discount must be greater than zero and at most 100.");
        }
    }

    private static void requireText(String key, JsonNode value) {
        if (!value.isTextual()) {
            throw invalid("Invalid " + key + ".");
        }
        String text = value.textValue();
        if (text.isBlank() || text.length() > MAX_TEXT_LENGTH || text.chars().anyMatch(Character::isISOControl)) {
            throw invalid("Invalid " + key + ".");
        }
    }

    private static void requireOrderNumber(String orderNumber) {
        if (!orderNumber.matches("[A-Za-z0-9_-]{1,80}")) {
            throw invalid("Invalid order number.");
        }
    }

    private static void requirePromotionWindow(JsonNode params) {
        try {
            LocalDateTime start = LocalDateTime.parse(params.path("startAt").asText());
            LocalDateTime end = LocalDateTime.parse(params.path("endAt").asText());
            if (!start.isBefore(end)) {
                throw invalid("startAt must be before endAt.");
            }
        } catch (DateTimeParseException ex) {
            throw invalid("Use ISO local dates for startAt and endAt.");
        }
    }

    private static AiFailure invalid(String message) {
        return new AiFailure("INVALID_PARAMETERS", AiExecutionStatus.NEEDS_INPUT, message);
    }
}

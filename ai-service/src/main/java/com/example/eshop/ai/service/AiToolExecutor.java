package com.example.eshop.ai.service;

import com.example.eshop.ai.client.CatalogToolClient;
import com.example.eshop.ai.client.OrderToolClient;
import com.example.eshop.ai.client.PaymentToolClient;
import com.example.eshop.ai.client.NotificationToolClient;
import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/** Calls one fixed client method. The tool name is the only dispatch key. */
@Service
@RequiredArgsConstructor
public class AiToolExecutor {

    private static final int LOW_STOCK_PAGE = 0;
    private static final int LOW_STOCK_SIZE = 20;
    private static final long DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final CatalogToolClient catalog;
    private final OrderToolClient orders;
    private final PaymentToolClient payments;
    private final NotificationToolClient notifications;
    private final AiToolRegistry registry;
    private final AiConfirmationService confirmations;
    private final AiKnowledgeService knowledge;
    private final com.example.eshop.ai.client.AuthToolClient auth;
    private final AiAuditLookupService auditLookup;

    public JsonNode execute(AiToolDefinition tool, JsonNode parameters, String bearer) {
        // Resolve the canonical definition again; callers cannot forge tool metadata.
        tool = registry.authorize(tool.toolName());
        registry.validate(tool, parameters);
        if (tool.risk() != com.example.eshop.ai.enums.AiToolRisk.READ_ONLY) {
            throw new AiFailure("CONFIRMATION_REQUIRED", AiExecutionStatus.DENIED, "Explicit confirmation is required.");
        }
        return dispatch(tool, parameters, bearer);
    }

    ConfirmedResult executeConfirmed(java.util.UUID confirmationId, String bearer) {
        var pending = confirmations.consume(confirmationId);
        var tool = registry.authorize(pending.intent());
        registry.validate(tool, pending.parameters());
        return new ConfirmedResult(tool, dispatch(tool, pending.parameters(), bearer));
    }

    record ConfirmedResult(AiToolDefinition tool, JsonNode data) {
    }

    JsonNode preview(AiToolDefinition tool, JsonNode parameters, String bearer) {
        tool = registry.authorize(tool.toolName());
        registry.validate(tool, parameters);
        if (tool.toolName() == com.example.eshop.ai.enums.AiIntent.ORDER_CANCEL) {
            JsonNode preview = orders.cancelPreview(bearer, parameters.path("orderNumber").asText());
            if (preview == null || !preview.path("eligible").asBoolean(false)) {
                throw new AiFailure("ORDER_NOT_CANCELLABLE", AiExecutionStatus.DENIED,
                        "This order is not eligible for cancellation.");
            }
            return preview;
        }
        if (tool.toolName() == com.example.eshop.ai.enums.AiIntent.PROMOTION_DISABLE) {
            return catalog.promotion(bearer, parameters.path("id").longValue());
        }
        return null;
    }

    private JsonNode dispatch(AiToolDefinition tool, JsonNode parameters, String bearer) {
        return switch (tool.toolName()) {
            case KNOWLEDGE_SEARCH -> knowledge.retrieve(parameters.path("query").textValue());
            case PRODUCT_GET -> catalog.product(bearer, parameters.path("id").longValue());
            case SKU_GET -> catalog.sku(bearer, parameters.path("skuId").longValue());
            case PRODUCT_SEARCH -> catalog.search(bearer, parameters.path("query").textValue());
            case INVENTORY_GET -> catalog.inventory(bearer, parameters.path("skuId").longValue());
            case INVENTORY_LOW_STOCK -> catalog.lowStock(
                    bearer,
                    parameters.path("threshold").asLong(DEFAULT_LOW_STOCK_THRESHOLD),
                    LOW_STOCK_PAGE,
                    LOW_STOCK_SIZE);
            case ORDER_GET -> orders.order(bearer, parameters.path("orderNumber").textValue());
            case MY_ORDERS -> orders.mine(bearer);
            case MY_ORDER_STATUS -> orders.latest(bearer);
            case MY_PAYMENT_STATUS -> payments.mine(bearer, parameters.path("id").longValue());
            case MY_RETURNS -> orders.returns(bearer);
            case MY_NOTIFICATIONS -> notifications.mine(bearer);
            case ADMIN_ORDER_LIST -> orders.admin(bearer, parameters.path("status").textValue(), date(parameters));
            case ADMIN_ORDER_SUMMARY -> orders.summary(bearer);
            case ADMIN_PAYMENT_LIST -> payments.admin(bearer, parameters.path("status").textValue(), date(parameters));
            case ADMIN_RETURN_LIST -> orders.adminReturns(bearer, parameters.path("status").textValue());
            case ADMIN_USER_LIST -> auth.users(bearer);
            case ADMIN_REVENUE_SUMMARY -> payments.revenue(bearer, date(parameters));
            case ADMIN_AUDIT_LOG -> auditLookup.find(parameters.path("requestId").textValue());
            case PROMOTION_GET -> catalog.promotion(bearer, parameters.path("id").longValue());
            case PROMOTION_CREATE -> catalog.createPromotion(bearer, parameters);
            case PROMOTION_DISABLE -> catalog.disablePromotion(bearer, parameters.path("id").longValue());
            case ORDER_CANCEL -> orders.cancel(bearer, parameters.path("orderNumber").asText());
            default -> throw new AiFailure("TOOL_DISABLED", AiExecutionStatus.DENIED, "Operation unavailable.");
        };
    }

    private static String date(JsonNode parameters) {
        String date = parameters.path("date").textValue();
        return "TODAY".equals(date)
                ? java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString() : date;
    }

}

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
    private final AiImageAttachment imageAttachment;
    private final AiImportService imports;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;

    public JsonNode execute(AiToolDefinition tool, JsonNode parameters, String bearer) {
        // Resolve the canonical definition again; callers cannot forge tool metadata.
        tool = registry.authorize(tool.toolName());
        registry.validate(tool, parameters);
        if (tool.risk() != com.example.eshop.ai.enums.AiToolRisk.READ_ONLY) {
            throw new AiFailure("CONFIRMATION_REQUIRED", AiExecutionStatus.DENIED, "Explicit confirmation is required.");
        }
        return dispatch(tool, parameters, bearer, null);
    }

    ConfirmedResult executeConfirmed(java.util.UUID confirmationId, String bearer) {
        var pending = confirmations.consume(confirmationId);
        var tool = registry.authorize(pending.intent());
        registry.validate(tool, pending.parameters());
        return new ConfirmedResult(tool, dispatch(tool, pending.parameters(), bearer, confirmationId.toString()));
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
        if (tool.toolName() == com.example.eshop.ai.enums.AiIntent.ADMIN_IMPORT_DRAFT) {
            var row=com.example.eshop.ai.registry.AiDraftArguments.parse(parameters);
            return mapper.valueToTree(java.util.Map.of("operation",row.tool().name(),"proposedValues",row.values(),"nextStep","Validate and submit this draft for independent review before execution."));
        }
        if (tool.toolName() == com.example.eshop.ai.enums.AiIntent.ORDER_CREATE) return orders.review(bearer, parameters.path("reviewId").asText());
        if (tool.toolName() == com.example.eshop.ai.enums.AiIntent.PROMOTION_DISABLE) {
            return catalog.promotion(bearer, parameters.path("id").longValue());
        }
        return null;
    }

    private JsonNode dispatch(AiToolDefinition tool, JsonNode parameters, String bearer, String key) {
        return switch (tool.toolName()) {
            case ADMIN_IMPORT_DRAFT -> mapper.valueToTree(imports.stage(com.example.eshop.ai.registry.AiDraftArguments.parse(parameters)));
            case ADMIN_PROMOTION_LIST -> requirePromotionData(catalog.promotions(bearer,parameters.path("status").asText(null),1,20));
            case ADMIN_PROMOTION_SKUS -> requirePromotionData(catalog.promotionSkus(bearer,parameters.path("id").asLong(),1,100));
            case PRODUCT_VISUAL_SEARCH -> visualSearch(bearer, parameters);
            case KNOWLEDGE_SEARCH -> knowledge.retrieve(parameters.path("query").textValue());
            case PRODUCT_GET, PRODUCT_DETAIL, PRODUCT_VARIANTS -> catalog.product(bearer, parameters.path("id").longValue());
            case SKU_GET, PRODUCT_STOCK_CHECK -> catalog.sku(bearer, parameters.path("skuId").longValue());
            case PRODUCT_SEARCH -> catalog.search(bearer, parameters.path("query").textValue(),
                    parameters.hasNonNull("minPrice") ? parameters.get("minPrice").decimalValue() : null,
                    parameters.hasNonNull("maxPrice") ? parameters.get("maxPrice").decimalValue() : null,
                    parameters.hasNonNull("inStock") ? parameters.get("inStock").booleanValue() : null);
            case INVENTORY_GET -> catalog.inventory(bearer, parameters.path("skuId").longValue());
            case INVENTORY_LOW_STOCK -> catalog.lowStock(
                    bearer,
                    parameters.path("threshold").asLong(DEFAULT_LOW_STOCK_THRESHOLD),
                    LOW_STOCK_PAGE,
                    LOW_STOCK_SIZE);
            case CART_GET -> orders.cart(bearer);
            case CHECKOUT_PREVIEW -> orders.checkoutPreview(bearer, parameters.path("addressId").longValue(), parameters.path("paymentMethod").asText());
            case CART_ADD, CART_UPDATE, CART_REMOVE -> orders.changeCart(bearer, key,
                    tool.toolName().name().substring(5).toLowerCase(java.util.Locale.ROOT), parameters);
            case ORDER_CREATE -> createOrder(bearer, parameters);
            case PAYMENT_INITIATE_KHQR, PAYMENT_GET_STATUS -> payments.orderQr(bearer, parameters.path("orderId").longValue());
            case ORDER_GET -> orders.order(bearer, parameters.path("orderNumber").textValue());
            case MY_ORDERS, ORDER_LIST -> orders.mine(bearer);
            case MY_ORDER_STATUS -> orders.latest(bearer);
            case MY_PAYMENT_STATUS -> payments.mine(bearer, parameters.path("id").longValue());
            case MY_RETURNS, RETURN_GET_STATUS -> orders.returns(bearer);
            case MY_NOTIFICATIONS, NOTIFICATION_LIST -> notifications.mine(bearer);
            case ADMIN_ORDER_LIST -> orders.admin(bearer, parameters.path("status").textValue(), date(parameters));
            case ADMIN_ORDER_SUMMARY -> orders.summary(bearer);
            case ADMIN_PAYMENT_LIST -> payments.admin(bearer, parameters.path("status").textValue(), date(parameters));
            case ADMIN_RETURN_LIST -> orders.adminReturns(bearer, parameters.path("status").textValue());
            case ADMIN_USER_LIST -> auth.users(bearer);
            case ADMIN_REVENUE_SUMMARY -> payments.revenue(bearer, date(parameters));
            case ADMIN_AUTOMATION_REVIEW -> orders.automationReview(bearer,parameters.path("id").longValue());
            case ADMIN_AUDIT_LOG -> auditLookup.find(parameters.path("requestId").textValue());
            case PROMOTION_GET -> catalog.promotion(bearer, parameters.path("id").longValue());
            case PROMOTION_CREATE -> catalog.createPromotion(bearer, parameters);
            case PROMOTION_DISABLE -> catalog.disablePromotion(bearer, parameters.path("id").longValue());
            case ORDER_CANCEL -> orders.cancel(bearer, parameters.path("orderNumber").asText());
            default -> throw new AiFailure("TOOL_DISABLED", AiExecutionStatus.DENIED, "Operation unavailable.");
        };
    }

    private JsonNode requirePromotionData(JsonNode response){
        if(response==null||!"200".equals(response.path("code").asText())||!response.hasNonNull("data"))throw new AiFailure("PROMOTION_LOOKUP_FAILED",AiExecutionStatus.FAILURE,"Promotion lookup failed.");
        return response.path("data");
    }
    private JsonNode visualSearch(String bearer, JsonNode parameters) {
        var response = catalog.recognize(bearer, imageAttachment.require(parameters.path("uploadId").asText()));
        if (response == null || !response.path("data").isObject() || !"200".equals(response.path("code").asText()))
            throw new AiFailure("VISUAL_SEARCH_UNAVAILABLE",AiExecutionStatus.FAILURE,"Photo recognition is unavailable. Search by product name.");
        if ("AI_UNAVAILABLE".equals(response.path("data").path("matchType").asText()))
            throw new AiFailure("VISUAL_SEARCH_UNAVAILABLE",AiExecutionStatus.FAILURE,"Photo recognition is unavailable. Search by product name.");
        return response.path("data");
    }

    private JsonNode createOrder(String bearer, JsonNode parameters) {
        var result = orders.createOrder(bearer, parameters.path("reviewId").asText());
        if (result != null && java.util.Set.of("FAILED", "CANCELLED").contains(result.path("status").asText()))
            throw new AiFailure("ORDER_NOT_CREATED", AiExecutionStatus.FAILURE, "The order could not be completed. Review current prices and stock before trying again.");
        return result;
    }

    private static String date(JsonNode parameters) {
        String date = parameters.path("date").textValue();
        return "TODAY".equals(date)
                ? java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok")).toString() : date;
    }

}

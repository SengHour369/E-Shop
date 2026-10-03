package com.example.eshop.ai.service;
import com.example.eshop.ai.client.*;
import com.example.eshop.ai.registry.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service @RequiredArgsConstructor
public class AiToolExecutor {
    private final CatalogToolClient catalog;
    private final OrderToolClient orders;
    public JsonNode execute(AiToolDefinition tool, JsonNode p, String bearer) {
        return switch(tool.toolName()) {
            case PRODUCT_GET -> catalog.product(bearer, p.path("id").longValue());
            case SKU_GET -> catalog.sku(bearer, p.path("skuId").longValue());
            case PRODUCT_SEARCH -> catalog.search(bearer, Map.of("criteria_type", 1, "criteria_value", p.path("query").textValue(), "page", 1, "size", 20));
            case INVENTORY_GET -> catalog.inventory(bearer, p.path("skuId").longValue());
            case INVENTORY_LOW_STOCK -> catalog.lowStock(bearer, p.path("threshold").asLong(10), 0, 20);
            case ORDER_GET -> orders.order(bearer, p.path("orderNumber").textValue());
            case PROMOTION_GET -> catalog.promotion(bearer, p.path("id").longValue());
            case PROMOTION_CREATE -> catalog.createPromotion(bearer, p);
            default -> throw new AiFailure("TOOL_DISABLED", com.example.eshop.ai.enums.AiExecutionStatus.DENIED, "Operation unavailable.");
        };
    }
}

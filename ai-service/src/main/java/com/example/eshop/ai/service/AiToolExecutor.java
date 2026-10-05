package com.example.eshop.ai.service;

import com.example.eshop.ai.client.CatalogToolClient;
import com.example.eshop.ai.client.OrderToolClient;
import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.registry.AiToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Calls one fixed client method. The tool name is the only dispatch key. */
@Service
@RequiredArgsConstructor
public class AiToolExecutor {

    private static final int SEARCH_PAGE = 1;
    private static final int SEARCH_SIZE = 20;
    private static final int LOW_STOCK_PAGE = 0;
    private static final int LOW_STOCK_SIZE = 20;
    private static final long DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final CatalogToolClient catalog;
    private final OrderToolClient orders;

    public JsonNode execute(AiToolDefinition tool, JsonNode parameters, String bearer) {
        return switch (tool.toolName()) {
            case PRODUCT_GET -> catalog.product(bearer, parameters.path("id").longValue());
            case SKU_GET -> catalog.sku(bearer, parameters.path("skuId").longValue());
            case PRODUCT_SEARCH -> catalog.search(bearer, searchRequest(parameters));
            case INVENTORY_GET -> catalog.inventory(bearer, parameters.path("skuId").longValue());
            case INVENTORY_LOW_STOCK -> catalog.lowStock(
                    bearer,
                    parameters.path("threshold").asLong(DEFAULT_LOW_STOCK_THRESHOLD),
                    LOW_STOCK_PAGE,
                    LOW_STOCK_SIZE);
            case ORDER_GET -> orders.order(bearer, parameters.path("orderNumber").textValue());
            case PROMOTION_GET -> catalog.promotion(bearer, parameters.path("id").longValue());
            case PROMOTION_CREATE -> catalog.createPromotion(bearer, parameters);
            default -> throw new AiFailure("TOOL_DISABLED", AiExecutionStatus.DENIED, "Operation unavailable.");
        };
    }

    private static Map<String, Object> searchRequest(JsonNode parameters) {
        return Map.of(
                "criteria_type", 1,
                "criteria_value", parameters.path("query").textValue(),
                "page", SEARCH_PAGE,
                "size", SEARCH_SIZE);
    }
}

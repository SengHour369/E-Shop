package com.example.eshop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "catalog-service")
public interface CatalogToolClient {

    @PostMapping("/internal/ai/promotions/{id}/disable")
    JsonNode disablePromotion(@RequestHeader("Authorization") String token, @PathVariable("id") long id);

    @GetMapping("/internal/ai/products/{id}")
    JsonNode product(@RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("id") long id);

    @GetMapping("/internal/ai/skus/{id}")
    JsonNode sku(@RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("id") long id);

    @GetMapping("/internal/ai/products")
    JsonNode search(@RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam("query") String query);

    @GetMapping("/internal/ai/inventory/{skuId}")
    JsonNode inventory(@RequestHeader("Authorization") String token, @PathVariable("skuId") long id);

    @GetMapping("/internal/ai/inventory/low-stock")
    JsonNode lowStock(@RequestHeader("Authorization") String token,
            @RequestParam("threshold") long threshold,
            @RequestParam("page") int page,
            @RequestParam("size") int size);

    @GetMapping("/internal/ai/promotions/{id}")
    JsonNode promotion(@RequestHeader("Authorization") String token, @PathVariable("id") long id);

    @PostMapping("/internal/ai/promotions")
    JsonNode createPromotion(@RequestHeader("Authorization") String token, @RequestBody JsonNode draft);
}

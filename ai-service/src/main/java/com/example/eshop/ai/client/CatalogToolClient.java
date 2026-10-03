package com.example.eshop.ai.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
@FeignClient(name="catalog-service")
public interface CatalogToolClient {
    @GetMapping("/internal/ai/products/{id}") JsonNode product(@RequestHeader("Authorization") String token, @PathVariable("id") long id);
    @GetMapping("/internal/ai/skus/{id}") JsonNode sku(@RequestHeader("Authorization") String token, @PathVariable("id") long id);
    @PostMapping("/api/v1/products/get/all") JsonNode search(@RequestHeader("Authorization") String token, @RequestBody Map<String,Object> request);
    @PostMapping("/api/v1/inventory/sku/") JsonNode inventory(@RequestHeader("Authorization") String token, @RequestParam("skuId") long id);
    @PostMapping("/api/v1/inventory/low-stock") JsonNode lowStock(@RequestHeader("Authorization") String token, @RequestParam("threshold") long threshold, @RequestParam("page") int page, @RequestParam("size") int size);
    @GetMapping("/api/v1/admin/promotions/{id}") JsonNode promotion(@RequestHeader("Authorization") String token, @PathVariable("id") long id);
    @PostMapping("/internal/ai/promotions") JsonNode createPromotion(@RequestHeader("Authorization") String token, @RequestBody JsonNode draft);
}

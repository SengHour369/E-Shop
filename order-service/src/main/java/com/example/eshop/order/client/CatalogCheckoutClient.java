package com.example.eshop.order.client;
import com.example.eshop.common.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name = "catalog-service", url = "${catalog-service.url:http://localhost:8082}")
public interface CatalogCheckoutClient {
    @PostMapping("/internal/catalog/checkouts/quote")
    CheckoutResult quote(@RequestHeader("Authorization") String token, @RequestBody CheckoutRequest request);
    @PostMapping("/internal/catalog/checkouts")
    CheckoutResult reserve(@RequestHeader("Authorization") String token, @RequestBody CheckoutRequest request);
    @PostMapping("/internal/catalog/checkouts/{id}/confirm")
    CheckoutResult confirm(@RequestHeader("Authorization") String token, @PathVariable("id") Long id);
    @PostMapping("/internal/catalog/checkouts/{id}/release")
    void release(@RequestHeader("Authorization") String token, @PathVariable("id") Long id);
}


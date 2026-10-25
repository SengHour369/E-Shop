package com.example.eshop.order.client;

import com.example.eshop.common.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "catalog-service",
        url = "${catalog-service.url:http://localhost:8082}"
)
public interface CatalogCheckoutClient {
    @PostMapping("/api/v1/products/reports/sku-categories")
    java.util.Map<String,String> salesCategories(@RequestHeader("Authorization") String token,@RequestBody java.util.List<Long> ids);

    @org.springframework.web.bind.annotation.PostMapping("/internal/commerce/guest/reviews")
    java.util.Map<String,Object> guestReview(@org.springframework.web.bind.annotation.RequestHeader("Authorization") String token,
            @org.springframework.web.bind.annotation.RequestBody com.example.eshop.common.storefront.GuestReviewSubmission request);


    @GetMapping("/internal/catalog/checkouts/{id}")
    CheckoutResult snapshot(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id
    );

    @PostMapping("/internal/catalog/checkouts/quote")
    CheckoutResult quote(
            @RequestHeader("Authorization") String token,
            @RequestBody CheckoutRequest request
    );

    @PostMapping("/internal/catalog/checkouts")
    CheckoutResult reserve(
            @RequestHeader("Authorization") String token,
            @RequestBody CheckoutRequest request
    );

    @PostMapping("/internal/catalog/checkouts/{id}/confirm")
    CheckoutResult confirm(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id
    );

    @PostMapping("/internal/catalog/checkouts/{id}/release")
    void release(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") Long id
    );
}
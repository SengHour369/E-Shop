package com.example.eshop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "payment-service")
public interface PaymentToolClient {

    @GetMapping("/internal/ai/payments/revenue")
    JsonNode revenue(@RequestHeader("Authorization") String token, @RequestParam("date") String date);

    @GetMapping("/internal/ai/payments/{id}")
    JsonNode mine(@RequestHeader("Authorization") String token, @PathVariable("id") long id);

    @GetMapping("/internal/ai/payments/admin")
    JsonNode admin(@RequestHeader("Authorization") String token,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String date);
}

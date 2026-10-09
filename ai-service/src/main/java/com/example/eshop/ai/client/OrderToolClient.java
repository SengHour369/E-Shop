package com.example.eshop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderToolClient {

    @GetMapping("/internal/ai/orders/{number}/cancel-preview")
    JsonNode cancelPreview(@RequestHeader("Authorization") String token, @PathVariable("number") String number);

    @org.springframework.web.bind.annotation.PostMapping("/internal/ai/orders/{number}/cancel")
    JsonNode cancel(@RequestHeader("Authorization") String token, @PathVariable("number") String number);

    @GetMapping("/internal/ai/orders")
    JsonNode mine(@RequestHeader("Authorization") String token);

    @GetMapping("/internal/ai/orders/latest")
    JsonNode latest(@RequestHeader("Authorization") String token);

    @GetMapping("/internal/ai/orders/admin")
    JsonNode admin(@RequestHeader("Authorization") String token,
            @org.springframework.web.bind.annotation.RequestParam(value = "status", required = false) String status,
            @org.springframework.web.bind.annotation.RequestParam(value = "date", required = false) String date);

    @GetMapping("/internal/ai/orders/summary")
    JsonNode summary(@RequestHeader("Authorization") String token);

    @GetMapping("/internal/ai/returns")
    JsonNode returns(@RequestHeader("Authorization") String token);

    @GetMapping("/internal/ai/returns/admin")
    JsonNode adminReturns(@RequestHeader("Authorization") String token,
            @org.springframework.web.bind.annotation.RequestParam(value = "status", required = false) String status);

    @GetMapping("/internal/ai/orders/{number}")
    JsonNode order(@RequestHeader("Authorization") String token, @PathVariable("number") String number);
}

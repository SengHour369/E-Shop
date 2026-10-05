package com.example.eshop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderToolClient {

    @GetMapping("/internal/ai/orders/{number}")
    JsonNode order(@RequestHeader("Authorization") String token, @PathVariable("number") String number);
}

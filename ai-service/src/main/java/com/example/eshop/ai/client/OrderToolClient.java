package com.example.eshop.ai.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
@FeignClient(name="order-service")
public interface OrderToolClient {
    @GetMapping("/internal/ai/orders/{number}") JsonNode order(@RequestHeader("Authorization") String token, @PathVariable("number") String number);
}

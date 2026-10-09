package com.example.eshop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service")
public interface AuthToolClient {

    @GetMapping("/internal/ai/users")
    JsonNode users(@RequestHeader("Authorization") String bearer);
}

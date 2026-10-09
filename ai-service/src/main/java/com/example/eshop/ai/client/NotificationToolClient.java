package com.example.eshop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service")
public interface NotificationToolClient {

    @GetMapping("/api/notifications?page=1&size=20")
    JsonNode mine(@RequestHeader("Authorization") String token);
}

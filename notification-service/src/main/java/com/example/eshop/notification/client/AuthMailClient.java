package com.example.eshop.notification.client;
import com.example.eshop.common.notification.NotificationEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="auth-service")
public interface AuthMailClient {
    @PostMapping("/internal/notifications/email")
    void send(@RequestHeader("Authorization") String bearer, @RequestBody NotificationEvent event);
}

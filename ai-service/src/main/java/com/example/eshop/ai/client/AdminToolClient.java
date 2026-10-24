package com.example.eshop.ai.client;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
/** Fixed read-only admin-service endpoints; the caller JWT is forwarded unchanged. */
@FeignClient(name="admin-service")
public interface AdminToolClient {
    @GetMapping("/api/v1/admin/monitor")
    JsonNode monitor(@RequestHeader("Authorization") String bearer);
    @GetMapping("/api/v1/admin/storefront/access-settings")
    JsonNode storefrontAccess(@RequestHeader("Authorization") String bearer);
}

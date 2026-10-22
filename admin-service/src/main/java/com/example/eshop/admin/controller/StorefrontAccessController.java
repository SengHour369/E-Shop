package com.example.eshop.admin.controller;

import com.example.eshop.admin.service.StorefrontAccessService;
import com.example.eshop.common.storefront.StorefrontAccess;
import com.example.eshop.common.security.LivePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

@RestController @RequiredArgsConstructor
public class StorefrontAccessController {
    private final StorefrontAccessService service;
    private final LivePermissionService permissions;
    @GetMapping("/api/v1/storefront/access-settings")
    public ResponseEntity<StorefrontAccess> publicSettings() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.current());
    }
    @GetMapping("/api/v1/admin/storefront/access-settings")
    public StorefrontAccess adminSettings() {
        permissions.require("STOREFRONT_ACCESS_VIEW"); return service.current();
    }
    @PutMapping("/api/v1/admin/storefront/access-settings")
    public StorefrontAccess update(@RequestBody StorefrontAccess request) { return service.save(request); }
}

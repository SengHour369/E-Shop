package com.example.eshop.catalog.controller;
import com.example.eshop.common.dto.*;
import com.example.eshop.catalog.service.impl.CatalogCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/internal/catalog/checkouts")
@RequiredArgsConstructor @PreAuthorize("hasAuthority('SERVICE_ORDER')")
public class CatalogCheckoutController {
    private final CatalogCheckoutService service;
    @PostMapping("/quote") public CheckoutResult quote(@RequestBody CheckoutRequest request) { return service.quote(request); }
    @PostMapping public CheckoutResult reserve(@RequestBody CheckoutRequest request) { return service.reserve(request); }
    @PostMapping("/{orderId}/confirm") public CheckoutResult confirm(@PathVariable Long orderId) { return service.confirm(orderId); }
    @PostMapping("/{orderId}/release") public void release(@PathVariable Long orderId) { service.release(orderId); }
}


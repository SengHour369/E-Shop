package com.example.eshop.order.controller;

import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.order.service.impl.AiShoppingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/internal/ai/shopping")
@RequiredArgsConstructor
public class AiShoppingController {

    private final AiShoppingService shopping;
    private final LivePermissionService permissions;

    public record CartChange(
            @Positive Long skuId,
            @Positive Long cartItemId,
            @Positive Long quantity
    ) {}

    @GetMapping("/cart")
    public Object cart() {
        permissions.current();
        return shopping.cart();
    }

    @PostMapping("/cart/{action}")
    public Object change(
            @PathVariable String action,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CartChange change
    ) {
        permissions.current();
        return shopping.mutate(
                key,
                action,
                change.skuId(),
                change.cartItemId(),
                change.quantity()
        );
    }

    @GetMapping("/checkout-preview")
    public Object preview(
            @RequestParam long addressId,
            @RequestParam String paymentMethod
    ) {
        permissions.current();
        return shopping.preview(addressId, paymentMethod);
    }

    @GetMapping("/reviews/{id}")
    public Object review(@PathVariable UUID id) {
        permissions.current();
        return shopping.review(id);
    }

    @PostMapping("/orders/{id}")
    public Object create(@PathVariable UUID id) {
        permissions.current();
        return shopping.create(id);
    }
}
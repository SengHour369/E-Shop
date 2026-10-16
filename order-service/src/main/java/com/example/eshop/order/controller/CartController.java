package com.example.eshop.order.controller;

import com.example.eshop.order.service.CartService;
import com.example.eshop.order.dto.request.CartRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController extends BaseController {

    private final CartService cartService;
    private final com.example.eshop.common.security.LivePermissionService permissions;
    private final com.example.eshop.order.service.impl.AiShoppingService shopping;

    @PostMapping("/user/id")
    public ResponseEntity<ResponseErrorTemplate> getCartByUserId(@RequestParam Long userId) {
        ResponseErrorTemplate cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/user/id/get-or-create")
    public ResponseEntity<ResponseErrorTemplate> getOrCreateCart(@RequestParam Long userId) {
        ResponseErrorTemplate cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/user/id/items")
    public ResponseEntity<ResponseErrorTemplate> addItemToCart(
            @RequestParam Long userId,
            @Valid @RequestBody CartRequest request,
            @RequestHeader(value="Idempotency-Key",required=false) java.util.UUID key) {
        com.example.eshop.common.security.CurrentCustomer.require(userId);
        permissions.current();
        ResponseErrorTemplate cart = key == null ? cartService.addItemToCart(userId, request)
                : ResponseErrorTemplate.success("Cart updated", shopping.mutate(key,"add",request.getProductSkuId(),null,request.getQuantity()));
        return new ResponseEntity<>(cart, HttpStatus.CREATED);
    }

    @PostMapping("/user/id/items/cartItemId")
    public ResponseEntity<ResponseErrorTemplate> updateCartItem(
            @RequestParam Long userId,
            @RequestParam Long cartItemId,
            @Valid @RequestBody CartRequest request,
            @RequestHeader(value="Idempotency-Key",required=false) java.util.UUID key) {
        com.example.eshop.common.security.CurrentCustomer.require(userId);
        permissions.current();
        ResponseErrorTemplate cart = key == null ? cartService.updateCartItem(userId, cartItemId, request)
                : ResponseErrorTemplate.success("Cart updated", shopping.mutate(key,"update",request.getProductSkuId(),cartItemId,request.getQuantity()));
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/user/userId/items/cartItemId")
    public ResponseEntity<ResponseErrorTemplate> removeItemFromCart(
            @RequestParam Long userId,
            @RequestParam Long cartItemId,
            @RequestHeader(value="Idempotency-Key",required=false) java.util.UUID key) {
        com.example.eshop.common.security.CurrentCustomer.require(userId);
        permissions.current();
        ResponseErrorTemplate cart = key == null ? cartService.removeItemFromCart(userId, cartItemId)
                : ResponseErrorTemplate.success("Cart updated", shopping.mutate(key,"remove",null,cartItemId,null));
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/user/userId/clear")
    public ResponseEntity<ResponseErrorTemplate> clearCart(@RequestParam Long userId) {
        ResponseErrorTemplate cart = cartService.clearCart(userId);
        return ResponseEntity.ok(cart);
    }
}

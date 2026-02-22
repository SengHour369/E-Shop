package com.example.learning_spring_security.controller;

import com.example.learning_spring_security.Service.ServiceStructure.CartService;
import com.example.learning_spring_security.dto.Request.CartRequest;
import com.example.learning_spring_security.dto.Response.CartResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<ResponseErrorTemplate> getCartByUserId(@PathVariable Long userId) {
        ResponseErrorTemplate cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/user/{userId}/get-or-create")
    public ResponseEntity<ResponseErrorTemplate> getOrCreateCart(@PathVariable Long userId) {
        ResponseErrorTemplate cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/user/{userId}/items")
    public ResponseEntity<ResponseErrorTemplate> addItemToCart(
            @PathVariable Long userId,
            @Valid @RequestBody CartRequest request) {
        ResponseErrorTemplate cart = cartService.addItemToCart(userId, request);
        return new ResponseEntity<>(cart, HttpStatus.CREATED);
    }

    @PutMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<ResponseErrorTemplate> updateCartItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartRequest request) {
        ResponseErrorTemplate cart = cartService.updateCartItem(userId, cartItemId, request);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/user/{userId}/items/{cartItemId}")
    public ResponseEntity<ResponseErrorTemplate> removeItemFromCart(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) {
        ResponseErrorTemplate cart = cartService.removeItemFromCart(userId, cartItemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<ResponseErrorTemplate> clearCart(@PathVariable Long userId) {
        ResponseErrorTemplate cart = cartService.clearCart(userId);
        return ResponseEntity.ok(cart);
    }
}
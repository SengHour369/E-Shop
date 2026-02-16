package com.example.learning_spring_security.Service;

import com.example.learning_spring_security.dto.Request.CartRequest;
import com.example.learning_spring_security.dto.Response.CartResponse;

public interface CartService {
    CartResponse getCartByUserId(Long userId);
    CartResponse addItemToCart(Long userId, CartRequest request);
    CartResponse updateCartItem(Long userId, Long cartItemId, CartRequest request);
    CartResponse removeItemFromCart(Long userId, Long cartItemId);
    CartResponse clearCart(Long userId);
    CartResponse getOrCreateCart(Long userId);
}
package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.CartRequest;
import com.example.learning_spring_security.dto.Response.CartResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface CartService {
    ResponseErrorTemplate getCartByUserId(Long userId);
    ResponseErrorTemplate addItemToCart(Long userId, CartRequest request);
    ResponseErrorTemplate updateCartItem(Long userId, Long cartItemId, CartRequest request);
    ResponseErrorTemplate removeItemFromCart(Long userId, Long cartItemId);
    ResponseErrorTemplate clearCart(Long userId);
    ResponseErrorTemplate getOrCreateCart(Long userId);
}
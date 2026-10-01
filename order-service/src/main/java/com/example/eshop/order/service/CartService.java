package com.example.eshop.order.service;

import com.example.eshop.order.dto.request.CartRequest;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;

public interface CartService {
    ResponseErrorTemplate getCartByUserId(Long userId);
    ResponseErrorTemplate addItemToCart(Long userId, CartRequest request);
    ResponseErrorTemplate updateCartItem(Long userId, Long cartItemId, CartRequest request);
    ResponseErrorTemplate removeItemFromCart(Long userId, Long cartItemId);
    ResponseErrorTemplate clearCart(Long userId);
    ResponseErrorTemplate getOrCreateCart(Long userId);
}

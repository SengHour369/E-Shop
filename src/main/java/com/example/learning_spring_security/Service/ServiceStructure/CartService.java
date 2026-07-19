package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.CartRequest;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface CartService {

    @Cacheable(value = "carts", key = "#userId")
    ResponseErrorTemplate getCartByUserId(Long userId);

    @CacheEvict(value = "carts", key = "#userId")
    ResponseErrorTemplate addItemToCart(Long userId, CartRequest request);

    @CacheEvict(value = "carts", key = "#userId")
    ResponseErrorTemplate updateCartItem(Long userId, Long cartItemId, CartRequest request);

    @CacheEvict(value = "carts", key = "#userId")
    ResponseErrorTemplate removeItemFromCart(Long userId, Long cartItemId);

    @CacheEvict(value = "carts", key = "#userId")
    ResponseErrorTemplate clearCart(Long userId);

    @Cacheable(value = "carts", key = "#userId")
    ResponseErrorTemplate getOrCreateCart(Long userId);
}
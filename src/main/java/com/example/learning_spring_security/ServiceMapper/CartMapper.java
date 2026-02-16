package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Cart;
import com.example.learning_spring_security.ServiceMapper.CartItemMapper;
import com.example.learning_spring_security.dto.Response.CartResponse;

import java.util.stream.Collectors;

public class CartMapper {

    public static CartResponse toResponse(Cart cart) {
        if (cart == null) return null;

        return CartResponse.builder()
                .id(cart.getId())
                .totalPrice(cart.getTotalPrice())
                .totalItems(cart.getTotalItems())
                .items(cart.getCartItems().stream()
                        .map(CartItemMapper::toResponse)
                        .collect(Collectors.toList()))

                .build();
    }
}
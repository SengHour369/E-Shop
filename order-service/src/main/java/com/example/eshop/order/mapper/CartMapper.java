package com.example.eshop.order.mapper;

import com.example.eshop.order.constant.Constant;
import com.example.eshop.order.model.Cart;
import com.example.eshop.order.dto.response.CartResponse;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartMapper {
    private final CartItemMapper cartItemMapper;

    public ResponseErrorTemplate toResponse(Cart cart) {
        if (cart == null) return null;

        CartResponse cartResponse = CartResponse.builder()
                .id(cart.getId())
                .totalPrice(cart.getTotalPrice())
                .totalItems(cart.getTotalItems())
                .items(cart.getCartItems().stream()
                        .map(cartItemMapper::toResponse)
                        .collect(Collectors.toList()))
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, cartResponse);
    }
}

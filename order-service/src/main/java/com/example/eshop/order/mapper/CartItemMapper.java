package com.example.eshop.order.mapper;

import com.example.eshop.order.model.Cart;
import com.example.eshop.order.model.CartItem;
import com.example.eshop.order.dto.response.CartItemResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CartItemMapper {

    public static CartItem toEntity(Cart cart, Long productSkuId, Long quantity, BigDecimal unitPrice) {
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));

        return CartItem.builder()
                .cart(cart)
                .productSkuId(productSkuId)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();
    }

    public CartItemResponse toResponse(CartItem cartItem) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productSkuId(cartItem.getProductSkuId())
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getTotalPrice())
                .createdAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }
}

package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Cart;
import com.example.learning_spring_security.Model.CartItem;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.ServiceMapper.ProductSkuMapper;
import com.example.learning_spring_security.dto.Response.CartItemResponse;
import java.math.BigDecimal;

public class CartItemMapper {

    public static CartItem toEntity(Cart cart, ProductSku productSku, Long quantity) {
        BigDecimal totalPrice = productSku.getPrice().multiply(BigDecimal.valueOf(quantity));

        return CartItem.builder()
                .cart(cart)
                .productSku(productSku)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();
    }

    public static CartItemResponse toResponse(CartItem cartItem) {
        return CartItemResponse.builder()
                .productSku(ProductSkuMapper.toResponse(cartItem.getProductSku()))
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getTotalPrice())
                .build();
    }
}
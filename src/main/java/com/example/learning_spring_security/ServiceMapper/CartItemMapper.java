package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Cart;
import com.example.learning_spring_security.Model.CartItem;
import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.ServiceMapper.ProductSkuMapper;
import com.example.learning_spring_security.dto.Response.CartItemResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Service
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
        ProductSku productSku = cartItem.getProductSku();
        Product  product = productSku.getProduct();
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .image(product.getMainImage())
                .name(product.getName())
                .productSku(ProductSkuMapper.toResponse(cartItem.getProductSku()))
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getTotalPrice())
                .build();
    }
}
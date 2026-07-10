package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Cart;
import com.example.learning_spring_security.Model.CartItem;
import com.example.learning_spring_security.Model.Product;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Response.CartItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Service
@RequiredArgsConstructor
public class CartItemMapper {

    private final ProductMapper productMapper;
    public static CartItem toEntity(Cart cart, ProductSku productSku, Long quantity) {
        BigDecimal totalPrice = productSku.getPrice().multiply(BigDecimal.valueOf(quantity));

        return CartItem.builder()
                .cart(cart)
                .productSku(productSku)
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();
    }

    public  CartItemResponse toResponse(CartItem cartItem) {
        ProductSku productSku = cartItem.getProductSku();
        Product  product = productSku.getProduct();

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .image(String.valueOf(product.getImage()))
                .name(product.getName())
                .productSku(productMapper.toSkuResponse(cartItem.getProductSku()))
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getTotalPrice())
                .build();
    }
}
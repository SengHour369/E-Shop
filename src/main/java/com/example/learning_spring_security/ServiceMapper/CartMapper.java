package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.Cart;
import com.example.learning_spring_security.ServiceMapper.CartItemMapper;
import com.example.learning_spring_security.dto.Response.CartResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class CartMapper {
    private final CartItemMapper cartItemMapper;
    public  ResponseErrorTemplate toResponse(Cart cart) {
        if (cart == null) return null;

        CartResponse cartResponse = CartResponse.builder()
                .id(cart.getId())
                .totalPrice(cart.getTotalPrice())
                .totalItems(cart.getTotalItems())
                .items(cart.getCartItems().stream()
                        .map(cartItemMapper::toResponse)
                        .collect(Collectors.toList()))
                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, cartResponse);
    }
}
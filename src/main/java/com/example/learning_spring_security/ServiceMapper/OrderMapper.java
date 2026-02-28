package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.dto.Response.OrderResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import java.util.stream.Collectors;

public class OrderMapper {

    public static ResponseErrorTemplate toResponse(OrderDetail order) {
        if (order == null) return null;

        OrderResponse orderResponse =  OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(order.getOrderItems().stream()
                        .map(OrderItemMapper::toResponse)
                        .collect(Collectors.toList()))
                .payment(order.getPayment() != null ?
                        PaymentMapper.toResponse(order.getPayment()) : null)

                .build();
        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, orderResponse);
    }
}
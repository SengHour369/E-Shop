package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.dto.Response.OrderResponse;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderResponse toResponse(OrderDetail order) {
        if (order == null) return null;

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .totalAmount(order.getTotalAmount())
                .items(order.getOrderItems().stream()
                        .map(OrderItemMapper::toResponse)
                        .collect(Collectors.toList()))
                .payment(order.getPayment() != null ?
                        PaymentMapper.toResponse(order.getPayment()) : null)

                .build();
    }
}
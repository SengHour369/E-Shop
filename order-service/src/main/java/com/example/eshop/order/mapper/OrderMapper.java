package com.example.eshop.order.mapper;

import com.example.eshop.order.constant.Constant;
import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.dto.response.OrderResponse;
import com.example.eshop.order.dto.response.ResponseErrorTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public ResponseErrorTemplate toResponse(OrderDetail order) {
        if (order == null) return null;

        OrderResponse orderResponse = OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .customerId(order.getUserId())
                .shippingAddressId(order.getShippingAddressId())
                .items(order.getOrderItems().stream()
                        .map(orderItemMapper::toResponse)
                        .collect(Collectors.toList()))
                .paymentId(order.getPaymentId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();

        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, orderResponse);
    }
}

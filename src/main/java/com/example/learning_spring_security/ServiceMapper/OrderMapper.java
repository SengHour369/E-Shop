package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.dto.Response.AddressResponse;
import com.example.learning_spring_security.dto.Response.OrderResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import java.util.stream.Collectors;

public class OrderMapper {

    public static ResponseErrorTemplate toResponse(OrderDetail order) {
        if (order == null) return null;

        AddressResponse shippingAddress = null;
        if (order.getShippingAddress() != null) {
            shippingAddress = AddressResponse.builder()
                    .id(order.getShippingAddress().getId())
                    .addressLine1(order.getShippingAddress().getAddressLine1())
                    .city(order.getShippingAddress().getCity())
                    .zipCode(order.getShippingAddress().getZipCode())
                    .country(order.getShippingAddress().getCountry())
                    .isDefault(order.getShippingAddress().getIsDefault())
                    .build();
        }

        OrderResponse orderResponse = OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .customerId(order.getUser() != null ? order.getUser().getId() : null)
                .customerName(order.getUser() != null ? order.getUser().getUsername() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .shippingAddress(shippingAddress)
                .items(order.getOrderItems().stream()
                        .map(OrderItemMapper::toResponse)
                        .collect(Collectors.toList()))
                .payment(order.getPayment() != null ? PaymentMapper.toResponse(order.getPayment()) : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();

        return new ResponseErrorTemplate(Constant.SUC_MSG, Constant.SUC_CODE, orderResponse);
    }
}
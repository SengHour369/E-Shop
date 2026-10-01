package com.example.eshop.payment.mapper;

import com.example.eshop.payment.dto.request.PaymentRequest;
import com.example.eshop.payment.dto.response.PaymentResponse;
import com.example.eshop.payment.model.Payment;

import java.time.LocalDateTime;

public class PaymentMapper {

    public static Payment toEntity(PaymentRequest request) {
        return Payment.builder()
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .transactionId(request.getTransactionId())
                .paymentProvider(request.getPaymentProvider())
                .userId(request.getUserId())
                .status("PENDING")
                .paymentDate(LocalDateTime.now())
                .build();
    }

    public static PaymentResponse toResponse(Payment payment) {
        // Cross-service: orderNumber previously came from payment.getOrderDetail().getOrderNumber()
        // (order-service). TODO: cross-service call via Feign to order-service to resolve it.
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .orderNumber(null)
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .code(payment.getCode())
                .codeOrder(payment.getCodeOrder())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .paymentProvider(payment.getPaymentProvider())
                .build();
    }
}

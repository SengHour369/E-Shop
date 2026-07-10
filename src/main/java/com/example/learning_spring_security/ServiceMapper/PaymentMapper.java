package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Model.Payment;
import com.example.learning_spring_security.dto.Request.PaymentRequest;
import com.example.learning_spring_security.dto.Response.PaymentResponse;


import java.time.LocalDateTime;

public class PaymentMapper {

    public static Payment toEntity(PaymentRequest request) {
        return Payment.builder()
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .transactionId(request.getTransactionId())
                .paymentProvider(request.getPaymentProvider())
                .status("PENDING")
                .paymentDate(LocalDateTime.now())
                .build();
    }

    public static PaymentResponse toResponse(Payment payment) {
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .code(payment.getCode())
                .codeOrder(payment.getCodeOrder())
                .paymentProvider(payment.getPaymentProvider());

        if (payment.getOrderDetail() != null) {
            builder.orderId(payment.getOrderDetail().getId())
                    .orderNumber(payment.getOrderDetail().getOrderNumber());
        }

        return builder.build();
    }
}
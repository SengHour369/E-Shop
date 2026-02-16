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
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paymentProvider(payment.getPaymentProvider())
                .build();
    }
}
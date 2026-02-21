package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.PaymentRequest;
import com.example.learning_spring_security.dto.Response.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(Long orderId, PaymentRequest request);
    PaymentResponse getPaymentByOrderId(Long orderId);
    PaymentResponse getPaymentByTransactionId(String transactionId);
    PaymentResponse updatePaymentStatus(Long paymentId, String status, String transactionId);
}
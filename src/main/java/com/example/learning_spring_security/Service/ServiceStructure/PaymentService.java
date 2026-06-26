package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.PaymentRequest;
import com.example.learning_spring_security.dto.Response.PaymentResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(Long orderId, PaymentRequest request);
    PaymentResponse getPaymentByOrderId(Long orderId);
    PaymentResponse getPaymentByTransactionId(String transactionId);
    PaymentResponse updatePaymentStatus(Long paymentId, String status, String transactionId);

    List<PaymentResponse> getPaymentsByUserId(Long userId);
    Page<PaymentResponse> getPaymentHistory(Long userId, String status,
                                            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    PaymentResponse getPaymentDetailByUserId(Long userId, Long paymentId);
}
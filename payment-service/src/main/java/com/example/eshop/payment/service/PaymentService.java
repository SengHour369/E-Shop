package com.example.eshop.payment.service;

import com.example.eshop.payment.dto.request.GetPaymentRequest;
import com.example.eshop.payment.dto.request.PaymentRequest;
import com.example.eshop.payment.dto.response.PaymentResponse;
import com.example.eshop.payment.dto.response.ResponseErrorTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    ResponseErrorTemplate getPayments(GetPaymentRequest request);
    ResponseErrorTemplate getPaymentById(Long id);
    ResponseErrorTemplate getPaymentsByUser(Long userId);
    ResponseErrorTemplate getPaymentDetailByUser(Long userId, Long paymentId);
    ResponseErrorTemplate getPaymentByOrder(Long orderId);
    ResponseErrorTemplate getPaymentByTransaction(String transactionId);

    // internal / order flow methods
    PaymentResponse processPayment(Long orderId, PaymentRequest request);
    PaymentResponse updatePaymentStatus(Long paymentId, String status, String transactionId);

    // kept for backward compat with OrderServiceImpl (order-service)
    List<PaymentResponse> getPaymentsByUserId(Long userId);
    PaymentResponse getPaymentDetailByUserId(Long userId, Long paymentId);
    PaymentResponse getPaymentByOrderId(Long orderId);
    PaymentResponse getPaymentByTransactionId(String transactionId);
    Page<PaymentResponse> getPaymentHistory(Long userId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}

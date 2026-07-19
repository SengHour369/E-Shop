package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetPaymentRequest;
import com.example.learning_spring_security.dto.Request.PaymentRequest;
import com.example.learning_spring_security.dto.Response.PaymentResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    // Paginated – skip
    ResponseErrorTemplate getPayments(GetPaymentRequest request);

    @Cacheable(value = "payments", key = "#id")
    ResponseErrorTemplate getPaymentById(Long id);

    @Cacheable(value = "payments", key = "#userId + ':list'")
    ResponseErrorTemplate getPaymentsByUser(Long userId);

    @Cacheable(value = "payments", key = "#userId + ':' + #paymentId")
    ResponseErrorTemplate getPaymentDetailByUser(Long userId, Long paymentId);

    @Cacheable(value = "payments", key = "#orderId + ':order'")
    ResponseErrorTemplate getPaymentByOrder(Long orderId);

    @Cacheable(value = "payments", key = "#transactionId")
    ResponseErrorTemplate getPaymentByTransaction(String transactionId);

    @Caching(evict = {
            @CacheEvict(value = "payments", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true)
    })
    PaymentResponse processPayment(Long orderId, PaymentRequest request);

    @CacheEvict(value = "payments", key = "#paymentId")
    PaymentResponse updatePaymentStatus(Long paymentId, String status, String transactionId);

    // Keep for backward compat – already annotated as above
    List<PaymentResponse> getPaymentsByUserId(Long userId);
    PaymentResponse getPaymentDetailByUserId(Long userId, Long paymentId);
    PaymentResponse getPaymentByOrderId(Long orderId);
    PaymentResponse getPaymentByTransactionId(String transactionId);

    // Paginated – skip
    Page<PaymentResponse> getPaymentHistory(Long userId, String status, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
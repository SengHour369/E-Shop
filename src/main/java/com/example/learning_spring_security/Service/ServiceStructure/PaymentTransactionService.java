package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Enumeration.TransactionStatus;
import com.example.learning_spring_security.dto.Request.GetPaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionStatusUpdateRequest;
import com.example.learning_spring_security.dto.Response.PaymentTransactionResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public interface PaymentTransactionService {

    // Paginated – skip
    ResponseErrorTemplate getTransactions(GetPaymentTransactionRequest request);

    @Cacheable(value = "transactions", key = "#id")
    ResponseErrorTemplate getTransactionById(Long id);

    @Cacheable(value = "transactions", key = "#transactionNo")
    ResponseErrorTemplate getTransactionByNo(String transactionNo);

    @Cacheable(value = "transactions", key = "#orderId + ':order'")
    ResponseErrorTemplate getTransactionsByOrder(Long orderId);

    @Cacheable(value = "transactions", key = "#customerId + ':customer'")
    ResponseErrorTemplate getTransactionsByCustomer(Long customerId);

    @Cacheable(value = "transactions", key = "#transactionId + ':history'")
    ResponseErrorTemplate getTransactionStatusHistory(Long transactionId);

    @CacheEvict(value = "transactions", allEntries = true)
    PaymentTransactionResponse createTransaction(PaymentTransactionRequest request);

    @CacheEvict(value = "transactions", key = "#transactionId")
    PaymentTransactionResponse updateTransactionStatus(Long transactionId, PaymentTransactionStatusUpdateRequest request);

    PaymentTransactionResponse recordTransaction(PaymentTransactionRequest request,
                                                 TransactionStatus finalStatus,
                                                 String changedBy,
                                                 String reason);
}
package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.dto.Request.GetPaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionRequest;
import com.example.learning_spring_security.dto.Request.PaymentTransactionStatusUpdateRequest;
import com.example.learning_spring_security.dto.Response.PaymentTransactionResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

public interface PaymentTransactionService {

    ResponseErrorTemplate getTransactions(GetPaymentTransactionRequest request);

    ResponseErrorTemplate getTransactionById(Long id);

    ResponseErrorTemplate getTransactionByNo(String transactionNo);

    ResponseErrorTemplate getTransactionsByOrder(Long orderId);

    ResponseErrorTemplate getTransactionsByCustomer(Long customerId);

    ResponseErrorTemplate getTransactionStatusHistory(Long transactionId);

    PaymentTransactionResponse createTransaction(PaymentTransactionRequest request);

    PaymentTransactionResponse updateTransactionStatus(Long transactionId, PaymentTransactionStatusUpdateRequest request);
}

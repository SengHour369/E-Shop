package com.example.learning_spring_security.Service.ServiceStructure;

import com.example.learning_spring_security.Enumeration.TransactionStatus;
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

    /**
     * Create a transaction and, in one step, move it to {@code finalStatus} (recording the
     * status-history entry). Shared by every payment flow that needs to persist a
     * {@code PaymentTransaction} the moment money is confirmed received (or fails).
     * Runs in the caller's transaction, so a failure here rolls the caller back — we never
     * want a COMPLETED payment without its matching transaction row.
     */
    PaymentTransactionResponse recordTransaction(PaymentTransactionRequest request,
                                                 TransactionStatus finalStatus,
                                                 String changedBy,
                                                 String reason);
}
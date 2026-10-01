package com.example.eshop.payment.service;

import com.example.eshop.payment.enumeration.TransactionStatus;
import com.example.eshop.payment.dto.request.GetPaymentTransactionRequest;
import com.example.eshop.payment.dto.request.PaymentTransactionRequest;
import com.example.eshop.payment.dto.request.PaymentTransactionStatusUpdateRequest;
import com.example.eshop.payment.dto.response.PaymentTransactionResponse;
import com.example.eshop.payment.dto.response.ResponseErrorTemplate;

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

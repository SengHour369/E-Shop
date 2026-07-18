package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Enumeration.TransactionStatus;
import com.example.learning_spring_security.Model.PaymentTransaction;
import com.example.learning_spring_security.Model.PaymentTransactionStatusHistory;
import com.example.learning_spring_security.dto.Request.PaymentTransactionRequest;
import com.example.learning_spring_security.dto.Response.PaymentTransactionResponse;
import com.example.learning_spring_security.dto.Response.PaymentTransactionStatusHistoryResponse;

import java.util.List;

public class PaymentTransactionMapper {

    public static PaymentTransaction toEntity(PaymentTransactionRequest request, String transactionNo) {
        return PaymentTransaction.builder()
                .transactionNo(transactionNo)
                .order(request.getOrderId())
                .customer(request.getCustomerId())
                .paymentMethod(request.getPaymentMethod())
                .maskedAccount(request.getMaskedAccount())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(TransactionStatus.PENDING)
                .remarks(request.getRemarks())
                .build();
    }

    public static PaymentTransactionResponse toResponse(PaymentTransaction transaction) {
        return PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .transactionNo(transaction.getTransactionNo())
                .orderId(transaction.getOrder())
                .customerId(transaction.getCustomer())
                .paymentMethod(transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null)
                .maskedAccount(transaction.getMaskedAccount())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
                .remarks(transaction.getRemarks())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    public static PaymentTransactionResponse toResponse(PaymentTransaction transaction,
                                                        List<PaymentTransactionStatusHistory> history) {
        PaymentTransactionResponse response = toResponse(transaction);
        response.setStatusHistory(history.stream()
                .map(PaymentTransactionMapper::toHistoryResponse)
                .toList());
        return response;
    }

    public static PaymentTransactionStatusHistoryResponse toHistoryResponse(PaymentTransactionStatusHistory history) {
        return PaymentTransactionStatusHistoryResponse.builder()
                .id(history.getId())
                .transactionId(history.getTransaction())
                .oldStatus(history.getOldStatus() != null ? history.getOldStatus().name() : null)
                .newStatus(history.getNewStatus() != null ? history.getNewStatus().name() : null)
                .changedAt(history.getChangedAt())
                .changedBy(history.getChangedBy())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .build();
    }
}
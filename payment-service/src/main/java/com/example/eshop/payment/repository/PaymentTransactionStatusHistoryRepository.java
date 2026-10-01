package com.example.eshop.payment.repository;

import com.example.eshop.payment.model.PaymentTransactionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionStatusHistoryRepository extends JpaRepository<PaymentTransactionStatusHistory, Long> {

    List<PaymentTransactionStatusHistory> findByTransactionOrderByChangedAtDesc(Long transactionId);
}

package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.PaymentTransactionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTransactionStatusHistoryRepository extends JpaRepository<PaymentTransactionStatusHistory, Long> {

    List<PaymentTransactionStatusHistory> findByTransactionOrderByChangedAtDesc(Long transactionId);
}
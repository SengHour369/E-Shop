package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Enumeration.TransactionStatus;
import com.example.learning_spring_security.Model.PaymentTransaction;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionNo(String transactionNo);

    List<PaymentTransaction> findByOrder(Long orderId);

    Page<PaymentTransaction> findByOrder(Long orderId, Pageable pageable);

    List<PaymentTransaction> findByCustomer(Long customerId);

    Page<PaymentTransaction> findByCustomer(Long customerId, Pageable pageable);

    List<PaymentTransaction> findByStatus(TransactionStatus status);

    Page<PaymentTransaction> findByStatus(TransactionStatus status, Pageable pageable);

    boolean existsByTransactionNo(String transactionNo);

}
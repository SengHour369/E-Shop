package com.example.eshop.payment.repository;

import com.example.eshop.payment.enumeration.TransactionStatus;
import com.example.eshop.payment.model.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

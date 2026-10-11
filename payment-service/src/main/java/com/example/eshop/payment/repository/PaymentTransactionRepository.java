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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from PaymentTransaction t where t.id=:id")
    Optional<PaymentTransaction> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select t.id from PaymentTransaction t where t.qrMd5 is not null and (t.status=com.example.eshop.payment.enumeration.TransactionStatus.PENDING or (t.status=com.example.eshop.payment.enumeration.TransactionStatus.FAILED and t.expiresAt>:cutoff)) and (t.nextVerificationAt is null or t.nextVerificationAt<=:now) order by t.nextVerificationAt asc")
    List<Long> dueVerification(@org.springframework.data.repository.query.Param("now") java.time.Instant now, @org.springframework.data.repository.query.Param("cutoff") java.time.Instant cutoff, Pageable pageable);
    Optional<PaymentTransaction> findByTransactionNo(String transactionNo);

    List<PaymentTransaction> findByOrder(Long orderId);

    Page<PaymentTransaction> findByOrder(Long orderId, Pageable pageable);

    List<PaymentTransaction> findByCustomer(Long customerId);

    Page<PaymentTransaction> findByCustomer(Long customerId, Pageable pageable);

    List<PaymentTransaction> findByStatus(TransactionStatus status);

    Page<PaymentTransaction> findByStatus(TransactionStatus status, Pageable pageable);

    boolean existsByTransactionNo(String transactionNo);

}

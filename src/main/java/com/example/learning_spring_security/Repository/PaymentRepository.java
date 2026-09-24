package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);


    List<Payment> findByStatus(String status);

    Page<Payment> findByStatus(String status, Pageable pageable);

    List<Payment> findByPaymentMethod(String paymentMethod);

    Page<Payment> findByPaymentMethod(String paymentMethod, Pageable pageable);

    Optional<Payment> findByOrderDetailId(Long orderId);

    Page<Payment> findByOrderDetailId(Long orderId, Pageable pageable);

    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Payment> findByOrderDetailUserId(Long userId);

    Optional<Payment> findByIdAndOrderDetailUserId(Long paymentId, Long userId);

    Page<Payment> findByOrderDetailUserId(Long userId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.orderDetail.user.id = :userId " +
            "AND (CAST(:status AS string) IS NULL OR p.status = :status) " +
            "AND (CAST(:startDate AS LocalDateTime) IS NULL OR p.paymentDate >= :startDate) " +
            "AND (CAST(:endDate AS LocalDateTime) IS NULL OR p.paymentDate <= :endDate)")
    Page<Payment> findPaymentHistory(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentDate BETWEEN :startDate AND :endDate")
    Optional<Double> getTotalCompletedPaymentsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    boolean existsByCode(String code);
}
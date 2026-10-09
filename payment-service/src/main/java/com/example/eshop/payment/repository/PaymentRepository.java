package com.example.eshop.payment.repository;

import com.example.eshop.payment.model.Payment;
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

    @Query("""
            select new com.example.eshop.payment.dto.response.AiRevenueSummary(
                p.currency, count(p), sum(p.amount))
            from Payment p
            where (p.deleted = false or p.deleted is null)
              and p.status = 'COMPLETED'
              and p.paymentDate >= :start and p.paymentDate < :end
            group by p.currency
            order by p.currency
            """)
    List<com.example.eshop.payment.dto.response.AiRevenueSummary> assistantRevenue(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select p from Payment p
            where (:status is null or p.status = :status)
              and (p.deleted = false or p.deleted is null)
              and (cast(:start as LocalDateTime) is null or p.paymentDate >= :start)
              and (cast(:end as LocalDateTime) is null or p.paymentDate < :end)
            """)
    Page<Payment> findVisibleForAssistant(@Param("status") String status,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select new com.example.eshop.payment.dto.response.MonthlyReportResponse(
                month(e.paymentDate), count(e), sum(e.amount), e.currency)
            from Payment e
            where (e.deleted = false or e.deleted is null)
              and e.paymentDate >= :start and e.paymentDate < :end
              and e.status = 'COMPLETED'
            group by month(e.paymentDate), e.currency
            order by month(e.paymentDate), e.currency
            """)
    List<com.example.eshop.payment.dto.response.MonthlyReportResponse> monthlyReport(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    Optional<Payment> findByTransactionId(String transactionId);


    List<Payment> findByStatus(String status);

    Page<Payment> findByStatus(String status, Pageable pageable);

    List<Payment> findByPaymentMethod(String paymentMethod);

    Page<Payment> findByPaymentMethod(String paymentMethod, Pageable pageable);

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findByOrderId(Long orderId, Pageable pageable);

    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Payment> findByUserId(Long userId);

    Optional<Payment> findByIdAndUserId(Long paymentId, Long userId);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.userId = :userId " +
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

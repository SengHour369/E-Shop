package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Refund;
import com.example.learning_spring_security.dto.Response.RefundDetailResponse;
import com.example.learning_spring_security.dto.Response.RefundListResponse;
import com.example.learning_spring_security.dto.Response.RefundSummaryResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundId(String refundId);

    boolean existsByRefundId(String refundId);

    boolean existsByReturnId(String returnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Refund r WHERE r.refundId = :refundId")
    Optional<Refund> findByRefundIdForUpdate(@Param("refundId") String refundId);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.paymentTransactionId = :paymentTransactionId AND r.status = 'PROCESSED'")
    BigDecimal sumProcessedAmountByPaymentTransactionId(@Param("paymentTransactionId") Long paymentTransactionId);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundListResponse(
                r.refundId, o.orderNumber, u.fullName, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            """)
    Page<RefundListResponse> findAllRefunds(Pageable pageable);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundListResponse(
                r.refundId, o.orderNumber, u.fullName, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            WHERE r.refundId = :refundId
            """)
    Page<RefundListResponse> findByRefundIdForList(@Param("refundId") String refundId, Pageable pageable);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundListResponse(
                r.refundId, o.orderNumber, u.fullName, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            WHERE o.orderNumber = :orderNo
            """)
    Page<RefundListResponse> findByOrderNo(@Param("orderNo") String orderNo, Pageable pageable);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundListResponse(
                r.refundId, o.orderNumber, u.fullName, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :customerName, '%'))
            """)
    Page<RefundListResponse> findByCustomerNameContaining(@Param("customerName") String customerName, Pageable pageable);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundListResponse(
                r.refundId, o.orderNumber, u.fullName, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            WHERE r.status = :status
            """)
    Page<RefundListResponse> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundListResponse(
                r.refundId, o.orderNumber, u.fullName, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            WHERE r.requestedAt BETWEEN :fromDate AND :toDate
            """)
    Page<RefundListResponse> findByRequestedAtBetween(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundDetailResponse(
                r.refundId, o.orderNumber, r.customerId, u.fullName, u.email, r.paymentTransactionId, pt.transactionNo,
                pt.paymentMethod, r.amount, r.status, r.reason, r.remark, r.requestedAt, r.requestedBy,
                r.processedAt, r.processedBy
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            LEFT JOIN PaymentTransaction pt ON pt.id = r.paymentTransactionId
            WHERE r.refundId = :refundId
            """)
    Optional<RefundDetailResponse> findDetailByRefundId(@Param("refundId") String refundId);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.RefundSummaryResponse(
                COUNT(r),
                SUM(CASE WHEN r.status = 'PROCESSED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.status = 'PENDING' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.status = 'PROCESSED' THEN r.amount ELSE 0 END)
            )
            FROM Refund r
            """)
    RefundSummaryResponse getRefundSummary();
}
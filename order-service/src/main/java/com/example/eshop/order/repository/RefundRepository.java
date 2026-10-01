package com.example.eshop.order.repository;

import com.example.eshop.order.model.Refund;
import com.example.eshop.order.dto.response.RefundDetailResponse;
import com.example.eshop.order.dto.response.RefundListResponse;
import com.example.eshop.order.dto.response.RefundSummaryResponse;
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

    // customerName previously came from joining User — auth-service owns User now.
    // TODO: cross-service call via Feign to auth-service to enrich customer name.
    @Query("""
            SELECT new com.example.eshop.order.dto.response.RefundListResponse(
                r.refundId, o.orderNumber, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            """)
    Page<RefundListResponse> findAllRefunds(Pageable pageable);

    @Query("""
            SELECT new com.example.eshop.order.dto.response.RefundListResponse(
                r.refundId, o.orderNumber, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            WHERE r.refundId = :refundId
            """)
    Page<RefundListResponse> findByRefundIdForList(@Param("refundId") String refundId, Pageable pageable);

    @Query("""
            SELECT new com.example.eshop.order.dto.response.RefundListResponse(
                r.refundId, o.orderNumber, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            WHERE o.orderNumber = :orderNo
            """)
    Page<RefundListResponse> findByOrderNo(@Param("orderNo") String orderNo, Pageable pageable);

    @Query("""
            SELECT new com.example.eshop.order.dto.response.RefundListResponse(
                r.refundId, o.orderNumber, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            WHERE r.status = :status
            """)
    Page<RefundListResponse> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT new com.example.eshop.order.dto.response.RefundListResponse(
                r.refundId, o.orderNumber, r.requestedAt, r.amount, r.status
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            WHERE r.requestedAt BETWEEN :fromDate AND :toDate
            """)
    Page<RefundListResponse> findByRequestedAtBetween(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    // customerName/customerEmail (User) and transactionNo/paymentMethod (PaymentTransaction) previously
    // came from cross-service joins — auth-service/payment-service own those now.
    // TODO: cross-service calls via Feign to auth-service/payment-service to enrich these.
    @Query("""
            SELECT new com.example.eshop.order.dto.response.RefundDetailResponse(
                r.refundId, o.orderNumber, r.customerId, r.paymentTransactionId,
                r.amount, r.status, r.reason, r.remark, r.requestedAt, r.requestedBy,
                r.processedAt, r.processedBy
            )
            FROM Refund r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            WHERE r.refundId = :refundId
            """)
    Optional<RefundDetailResponse> findDetailByRefundId(@Param("refundId") String refundId);

    @Query("""
            SELECT new com.example.eshop.order.dto.response.RefundSummaryResponse(
                COUNT(r),
                SUM(CASE WHEN r.status = 'PROCESSED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.status = 'PENDING' THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.status = 'PROCESSED' THEN r.amount ELSE 0 END)
            )
            FROM Refund r
            """)
    RefundSummaryResponse getRefundSummary();

    // customerName filter previously fuzzy-matched via a User join.
    // TODO: cross-service call via Feign to auth-service if this filtering is still needed;
    // for now this falls back to returning all refunds.
    default Page<RefundListResponse> findByCustomerNameContaining(String customerName, Pageable pageable) {
        return findAllRefunds(pageable);
    }
}

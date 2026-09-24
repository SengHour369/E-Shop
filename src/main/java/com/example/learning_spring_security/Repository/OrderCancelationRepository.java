package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.OrderCancelation;
import com.example.learning_spring_security.dto.Response.CancelationDetailResponse;
import com.example.learning_spring_security.dto.Response.CancelationListResponse;
import com.example.learning_spring_security.dto.Response.CancelationSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderCancelationRepository extends JpaRepository<OrderCancelation, Long> {

    Optional<OrderCancelation> findByOrderNo(String orderNo);

    boolean existsByOrderId(Long orderId);

    boolean existsByCancelationId(String cancelationId);

    @Query("""
    SELECT new com.example.learning_spring_security.dto.Response.CancelationListResponse(
        oc.orderNo, oc.customerName, oc.cancelDate, oc.cancelReason, oc.amount
    )
    FROM OrderCancelation oc
    WHERE (:orderNo IS NULL OR oc.orderNo = :orderNo)
    AND (:customerName IS NULL OR LOWER(oc.customerName) LIKE LOWER(CONCAT('%', :customerName, '%')))
    AND (:cancelReason IS NULL OR oc.cancelReason = :cancelReason)
    AND (:cancelStatus IS NULL OR oc.cancelStatus = :cancelStatus)
    AND (:fromCancelDate IS NULL OR oc.cancelDate >= :fromCancelDate)
    AND (:toCancelDate IS NULL OR oc.cancelDate <= :toCancelDate)
    AND (:minAmount IS NULL OR oc.amount >= :minAmount)
    AND (:maxAmount IS NULL OR oc.amount <= :maxAmount)
    """)
    Page<CancelationListResponse> search(
            @Param("orderNo") String orderNo,
            @Param("customerName") String customerName,
            @Param("cancelReason") String cancelReason,
            @Param("cancelStatus") String cancelStatus,
            @Param("fromCancelDate") LocalDateTime fromCancelDate,
            @Param("toCancelDate") LocalDateTime toCancelDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable
    );

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.CancelationDetailResponse(
                oc.cancelationId, oc.orderId, oc.orderNo, oc.customerId, oc.customerName,
                oc.cancelReason, oc.cancelStatus, oc.cancelSource, oc.cancelDate, oc.amount,
                oc.currency, oc.remark, oc.reviewedAt, oc.reviewedBy,
                oc.createdAt, oc.createdBy, oc.updatedAt, oc.updatedBy
            )
            FROM OrderCancelation oc
            WHERE oc.orderNo = :orderNo
            """)
    Optional<CancelationDetailResponse> findDetailByOrderNo(@Param("orderNo") String orderNo);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.CancelationSummaryResponse(
                COUNT(oc),
                SUM(CASE WHEN oc.cancelStatus IN ('REQUESTED', 'PENDING_REVIEW') THEN 1 ELSE 0 END),
                COALESCE(SUM(oc.amount), 0)
            )
            FROM OrderCancelation oc
            """)
    CancelationSummaryResponse getCancelationSummary();
}
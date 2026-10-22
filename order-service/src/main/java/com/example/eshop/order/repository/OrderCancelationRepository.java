package com.example.eshop.order.repository;

import com.example.eshop.order.model.OrderCancelation;
import com.example.eshop.order.dto.response.CancelationDetailResponse;
import com.example.eshop.order.dto.response.CancelationListResponse;
import com.example.eshop.order.dto.response.CancelationSummaryResponse;
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
public interface OrderCancelationRepository extends JpaRepository<OrderCancelation, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<OrderCancelation> {

    Optional<OrderCancelation> findByOrderNo(String orderNo);

    boolean existsByOrderId(Long orderId);

    boolean existsByCancelationId(String cancelationId);

    default Page<CancelationListResponse> search(
            String orderNo, String customerName, String cancelReason, String cancelStatus,
            LocalDateTime fromCancelDate, LocalDateTime toCancelDate,
            BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<OrderCancelation> filters = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (orderNo != null) predicates.add(cb.equal(root.get("orderNo"), orderNo));
            if (customerName != null) predicates.add(cb.like(cb.lower(root.get("customerName")), "%" + customerName.toLowerCase(java.util.Locale.ROOT) + "%"));
            if (cancelReason != null) predicates.add(cb.equal(root.get("cancelReason"), cancelReason));
            if (cancelStatus != null) predicates.add(cb.equal(root.get("cancelStatus"), cancelStatus));
            if (fromCancelDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("cancelDate"), fromCancelDate));
            if (toCancelDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("cancelDate"), toCancelDate));
            if (minAmount != null) predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            if (maxAmount != null) predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return findAll(filters, pageable).map(row -> new CancelationListResponse(
                row.getOrderNo(), row.getCustomerName(), row.getCancelDate(), row.getCancelReason(), row.getAmount()));
    }

    @Query("""
            SELECT new com.example.eshop.order.dto.response.CancelationDetailResponse(
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
            SELECT new com.example.eshop.order.dto.response.CancelationSummaryResponse(
                COUNT(oc),
                SUM(CASE WHEN oc.cancelStatus IN ('REQUESTED', 'PENDING_REVIEW') THEN 1 ELSE 0 END),
                COALESCE(SUM(oc.amount), 0)
            )
            FROM OrderCancelation oc
            """)
    CancelationSummaryResponse getCancelationSummary();
}

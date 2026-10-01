package com.example.eshop.order.repository;

import com.example.eshop.order.model.Return;
import com.example.eshop.order.dto.response.ReturnDetailResponse;
import com.example.eshop.order.dto.response.ReturnListResponse;
import com.example.eshop.order.dto.response.ReturnSummaryResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<Return, Long> {

    Optional<Return> findByReturnId(String returnId);

    boolean existsByReturnId(String returnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Return r WHERE r.returnId = :returnId")
    Optional<Return> findByReturnIdForUpdate(@Param("returnId") String returnId);

    // customerName/productName previously came from joining User/Product — those entities live in
    // auth-service/catalog-service now, so this only carries local ids.
    // TODO: cross-service calls via Feign to auth-service/catalog-service to enrich these.
    @Query("""
            SELECT new com.example.eshop.order.dto.response.ReturnDetailResponse(
                r.returnId, o.orderNumber, r.customerId, r.productId,
                r.returnType, r.reason, r.status, r.amount, r.requestedAt, r.requestedBy,
                r.approvedAt, r.approvedBy, r.rejectedAt, r.rejectedBy, r.completedAt, r.remark
            )
            FROM Return r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            WHERE r.returnId = :returnId
            """)
    Optional<ReturnDetailResponse> findDetailByReturnId(@Param("returnId") String returnId);

    @Query("""
            SELECT new com.example.eshop.order.dto.response.ReturnSummaryResponse(
                COUNT(r),
                SUM(CASE WHEN r.status IN ('APPROVED', 'REJECTED', 'COMPLETED') THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.status = 'REQUESTED' THEN 1 ELSE 0 END)
            )
            FROM Return r
            """)
    ReturnSummaryResponse getReturnSummary();

    @Query("""
        SELECT new com.example.eshop.order.dto.response.ReturnListResponse(
            r.returnId, o.orderNumber, r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        WHERE r.returnId = :returnId
    """)
    Optional<ReturnListResponse> findReturnListByReturnId(@Param("returnId") String returnId);

    @Query("""
        SELECT new com.example.eshop.order.dto.response.ReturnListResponse(
            r.returnId, o.orderNumber, r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
    """)
    Page<ReturnListResponse> findAllReturns(Pageable pageable);

    @Query("""
        SELECT new com.example.eshop.order.dto.response.ReturnListResponse(
            r.returnId, o.orderNumber, r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        WHERE o.orderNumber = :orderNo
    """)
    Page<ReturnListResponse> findByOrderNumber(@Param("orderNo") String orderNo, Pageable pageable);

    @Query("""
        SELECT new com.example.eshop.order.dto.response.ReturnListResponse(
            r.returnId, o.orderNumber, r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        WHERE r.status = :status
    """)
    Page<ReturnListResponse> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("""
        SELECT new com.example.eshop.order.dto.response.ReturnListResponse(
            r.returnId, o.orderNumber, r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        WHERE r.returnType = :returnType
    """)
    Page<ReturnListResponse> findByReturnType(@Param("returnType") String returnType, Pageable pageable);

    // customerName/productName filters previously fuzzy-matched via User/Product joins.
    // TODO: cross-service calls via Feign to auth-service/catalog-service if this filtering is still needed;
    // for now these fall back to returning all returns.
    default Page<ReturnListResponse> findByCustomerNameContaining(String customerName, Pageable pageable) {
        return findAllReturns(pageable);
    }

    default Page<ReturnListResponse> findByProductNameContaining(String productName, Pageable pageable) {
        return findAllReturns(pageable);
    }
}

package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Return;
import com.example.learning_spring_security.dto.Response.ReturnDetailResponse;
import com.example.learning_spring_security.dto.Response.ReturnListResponse;
import com.example.learning_spring_security.dto.Response.ReturnSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<Return, Long> {

    Optional<Return> findByReturnId(String returnId);

    boolean existsByReturnId(String returnId);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
                r.returnId, o.orderNumber, u.fullName, p.name, r.returnType, r.reason, r.status, r.amount
            )
            FROM ReturnRequest r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            LEFT JOIN Product p ON p.id = r.productId
            WHERE (:returnId IS NULL OR r.returnId = :returnId)
            AND (:orderNo IS NULL OR o.orderNumber = :orderNo)
            AND (:customerName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :customerName, '%')))
            AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
            AND (:returnType IS NULL OR r.returnType = :returnType)
            AND (:status IS NULL OR r.status = :status)
            AND (:fromDate IS NULL OR r.requestedAt >= :fromDate)
            AND (:toDate IS NULL OR r.requestedAt <= :toDate)
            """)
    Page<ReturnListResponse> search(
            @Param("returnId") String returnId,
            @Param("orderNo") String orderNo,
            @Param("customerName") String customerName,
            @Param("productName") String productName,
            @Param("returnType") String returnType,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.ReturnDetailResponse(
                r.returnId, o.orderNumber, r.customerId, u.fullName, u.email, r.productId, p.name,
                r.returnType, r.reason, r.status, r.amount, r.requestedAt, r.requestedBy,
                r.approvedAt, r.approvedBy, r.rejectedAt, r.rejectedBy, r.completedAt, r.remark
            )
            FROM ReturnRequest r
            LEFT JOIN OrderDetail o ON o.id = r.orderId
            LEFT JOIN User u ON u.id = r.customerId
            LEFT JOIN Product p ON p.id = r.productId
            WHERE r.returnId = :returnId
            """)
    Optional<ReturnDetailResponse> findDetailByReturnId(@Param("returnId") String returnId);

    @Query("""
            SELECT new com.example.learning_spring_security.dto.Response.ReturnSummaryResponse(
                COUNT(r),
                SUM(CASE WHEN r.status IN ('APPROVED', 'REJECTED', 'COMPLETED') THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.status = 'REQUESTED' THEN 1 ELSE 0 END)
            )
            FROM ReturnRequest r
            """)
    ReturnSummaryResponse getReturnSummary();
}
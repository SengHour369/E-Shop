package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Return;
import com.example.learning_spring_security.dto.Response.ReturnDetailResponse;
import com.example.learning_spring_security.dto.Response.ReturnListResponse;
import com.example.learning_spring_security.dto.Response.ReturnSummaryResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<Return, Long> {

    Optional<Return> findByReturnId(String returnId);

    boolean existsByReturnId(String returnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Return r WHERE r.returnId = :returnId")
    Optional<Return> findByReturnIdForUpdate(@Param("returnId") String returnId);


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
    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
            r.returnId, o.orderNumber, u.fullName, p.name,
            r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        LEFT JOIN User u ON u.id = r.customerId
        LEFT JOIN Product p ON p.id = r.productId
        WHERE r.returnId = :returnId
    """)
    Optional<ReturnListResponse> findReturnListByReturnId(@Param("returnId") String returnId);

    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
            r.returnId, o.orderNumber, u.fullName, p.name,
            r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        LEFT JOIN User u ON u.id = r.customerId
        LEFT JOIN Product p ON p.id = r.productId
    """)
    Page<ReturnListResponse> findAllReturns(Pageable pageable);

    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
            r.returnId, o.orderNumber, u.fullName, p.name,
            r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        LEFT JOIN User u ON u.id = r.customerId
        LEFT JOIN Product p ON p.id = r.productId
        WHERE o.orderNumber = :orderNo
    """)
    Page<ReturnListResponse> findByOrderNumber(@Param("orderNo") String orderNo, Pageable pageable);

    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
            r.returnId, o.orderNumber, u.fullName, p.name,
            r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        LEFT JOIN User u ON u.id = r.customerId
        LEFT JOIN Product p ON p.id = r.productId
        WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :customerName, '%'))
    """)
    Page<ReturnListResponse> findByCustomerNameContaining(@Param("customerName") String customerName, Pageable pageable);

    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
            r.returnId, o.orderNumber, u.fullName, p.name,
            r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        LEFT JOIN User u ON u.id = r.customerId
        LEFT JOIN Product p ON p.id = r.productId
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))
    """)
    Page<ReturnListResponse> findByProductNameContaining(@Param("productName") String productName, Pageable pageable);

    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
            r.returnId, o.orderNumber, u.fullName, p.name,
            r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        LEFT JOIN User u ON u.id = r.customerId
        LEFT JOIN Product p ON p.id = r.productId
        WHERE r.status = :status
    """)
    Page<ReturnListResponse> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.ReturnListResponse(
            r.returnId, o.orderNumber, u.fullName, p.name,
            r.returnType, r.reason, r.status, r.amount
        )
        FROM Return r
        LEFT JOIN OrderDetail o ON o.id = r.orderId
        LEFT JOIN User u ON u.id = r.customerId
        LEFT JOIN Product p ON p.id = r.productId
        WHERE r.returnType = :returnType
    """)
    Page<ReturnListResponse> findByReturnType(@Param("returnType") String returnType, Pageable pageable);
}
package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.dto.Response.OrderStatusSummaryResponse;
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
public interface OrderRepository extends JpaRepository<OrderDetail, Long> {

    Page<OrderDetail> findByUserId(Long userId, Pageable pageable);

    Optional<OrderDetail> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.productSku WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM OrderDetail o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.productSku sku " +
            "LEFT JOIN FETCH sku.product " +
            "LEFT JOIN FETCH o.payment " +
            "LEFT JOIN FETCH o.shippingAddress " +
            "WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithFullDetail(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM OrderDetail o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.productSku sku " +
            "LEFT JOIN FETCH sku.product " +
            "LEFT JOIN FETCH o.payment " +
            "LEFT JOIN FETCH o.shippingAddress " +
            "WHERE o.orderNumber = :orderNumber")
    Optional<OrderDetail> findByOrderNumberWithFullDetail(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.payment WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithPayment(@Param("id") Long id);

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.shippingAddress WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithShippingAddress(@Param("id") Long id);

    @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.user.id = :userId AND o.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    List<OrderDetail> findByStatus(String status);

    Page<OrderDetail> findByStatus(String status, Pageable pageable);

    List<OrderDetail> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT o FROM OrderDetail o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<OrderDetail> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM OrderDetail o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.productSku sku " +
            "LEFT JOIN FETCH sku.product " +
            "LEFT JOIN FETCH o.payment " +
            "LEFT JOIN FETCH o.shippingAddress " +
            "WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<OrderDetail> findByIdAndUserIdWithFullDetail(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("SELECT o FROM OrderDetail o WHERE o.user.id = :userId " +
            "AND (CAST(:status AS string) IS NULL OR o.status = :status) " +
            "AND (CAST(:startDate AS LocalDateTime) IS NULL OR o.orderDate >= :startDate) " +
            "AND (CAST(:endDate AS LocalDateTime) IS NULL OR o.orderDate <= :endDate)")
    Page<OrderDetail> findOrderDetailHistory(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    @Query("""
        SELECT new com.example.learning_spring_security.dto.Response.OrderStatusSummaryResponse(
            COUNT(o),
            SUM(CASE WHEN o.status = 'PENDING' THEN 1 ELSE 0 END),
            SUM(CASE WHEN o.status = 'CONFIRMED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN o.status = 'PROCESSING' THEN 1 ELSE 0 END),
            SUM(CASE WHEN o.status = 'SHIPPED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN o.status = 'FAILED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN o.status = 'REFUNDED' THEN 1 ELSE 0 END)
        )
        FROM OrderDetail o
        """)
    OrderStatusSummaryResponse getOrderStatusSummary();
}
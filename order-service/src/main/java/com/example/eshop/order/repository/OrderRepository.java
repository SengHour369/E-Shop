package com.example.eshop.order.repository;

import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.dto.response.OrderStatusSummaryResponse;
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

    @Query("""
            select new com.example.eshop.order.dto.response.AiOrderStatusCount(o.status, count(o))
            from OrderDetail o
            where o.deleted = false or o.deleted is null
            group by o.status
            order by o.status
            """)
    List<com.example.eshop.order.dto.response.AiOrderStatusCount> assistantStatusSummary();

    @Query("select o from OrderDetail o where o.userId = :userId and (o.deleted = false or o.deleted is null)")
    Page<OrderDetail> findVisibleByUserId(@Param("userId") long userId, Pageable pageable);

    @Query("""
            select o from OrderDetail o
            where (:status is null or o.status = :status)
              and (o.deleted = false or o.deleted is null)
              and (cast(:start as LocalDateTime) is null or o.orderDate >= :start)
              and (cast(:end as LocalDateTime) is null or o.orderDate < :end)
            """)
    Page<OrderDetail> findVisibleForAssistant(@Param("status") String status,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            select new com.example.eshop.order.dto.response.DailyReportResponse(
                year(e.orderDate), month(e.orderDate), day(e.orderDate), count(e))
            from OrderDetail e
            where (e.deleted = false or e.deleted is null)
              and e.orderDate >= :start and e.orderDate < :end
              and e.status <> 'CHECKOUT_PENDING'
            group by year(e.orderDate), month(e.orderDate), day(e.orderDate)
            order by year(e.orderDate), month(e.orderDate), day(e.orderDate)
            """)
    List<com.example.eshop.order.dto.response.DailyReportResponse> dailyReport(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    @Query("""
            select new com.example.eshop.order.dto.response.MonthlyReportResponse(
                month(e.orderDate), count(e), sum(e.totalAmount), 'USD')
            from OrderDetail e
            where (e.deleted = false or e.deleted is null)
              and e.orderDate >= :start and e.orderDate < :end
              and e.status not in ('CANCELLED', 'FAILED', 'REFUNDED', 'CHECKOUT_PENDING')
            group by month(e.orderDate)
            order by month(e.orderDate)
            """)
    List<com.example.eshop.order.dto.response.MonthlyReportResponse> monthlyReport(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Optional<OrderDetail> findByUserIdAndCheckoutKey(Long userId, String checkoutKey);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderDetail o where o.id = :id")
    Optional<OrderDetail> lockById(@Param("id") Long id);
    @Query("select o.id from OrderDetail o where (o.status = 'CHECKOUT_PENDING' or (o.status = 'CANCELLED' and o.catalogCompleted = false)) and (o.checkoutRetryAt is null or o.checkoutRetryAt <= :now) order by o.id")
    List<Long> recoverable(@Param("now") LocalDateTime now, Pageable pageable);

    Page<OrderDetail> findByUserId(Long userId, Pageable pageable);

    Optional<OrderDetail> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.orderItems oi WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithItems(@Param("id") Long id);

    // Was findByIdWithFullDetail — the sku/product/payment/shippingAddress joins were dropped
    // since those are no longer local JPA relations (catalog-service/payment-service/auth-service own them).
    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.orderItems oi WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithFullDetail(@Param("id") Long id);

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.orderItems oi WHERE o.orderNumber = :orderNumber")
    Optional<OrderDetail> findByOrderNumberWithFullDetail(@Param("orderNumber") String orderNumber);

    @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.userId = :userId AND o.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    List<OrderDetail> findByStatus(String status);

    Page<OrderDetail> findByStatus(String status, Pageable pageable);

    List<OrderDetail> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT o FROM OrderDetail o WHERE o.userId = :userId ORDER BY o.orderDate DESC")
    List<OrderDetail> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.orderItems oi WHERE o.id = :orderId AND o.userId = :userId")
    Optional<OrderDetail> findByIdAndUserIdWithFullDetail(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("SELECT o FROM OrderDetail o WHERE o.userId = :userId " +
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
        SELECT new com.example.eshop.order.dto.response.OrderStatusSummaryResponse(
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

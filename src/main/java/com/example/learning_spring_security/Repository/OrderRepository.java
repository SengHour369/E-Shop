package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.OrderDetail;
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

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.payment WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithPayment(@Param("id") Long id);

    @Query("SELECT o FROM OrderDetail o LEFT JOIN FETCH o.shippingAddress WHERE o.id = :id")
    Optional<OrderDetail> findByIdWithShippingAddress(@Param("id") Long id);

    @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.user.id = :userId AND o.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    List<OrderDetail> findByStatus(String status);

    List<OrderDetail> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT o FROM OrderDetail o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<OrderDetail> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);
}
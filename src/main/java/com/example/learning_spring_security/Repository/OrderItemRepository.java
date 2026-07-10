package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderDetailId(Long orderId);

}
package com.example.eshop.order.repository;

import com.example.eshop.order.model.RefundStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundStatusHistoryRepository extends JpaRepository<RefundStatusHistory, Long> {

    List<RefundStatusHistory> findByRefundOrderByChangedAtDesc(Long refundId);
}

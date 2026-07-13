package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.RefundStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundStatusHistoryRepository extends JpaRepository<RefundStatusHistory, Long> {

    List<RefundStatusHistory> findByRefundOrderByChangedAtDesc(Long refundId);
}
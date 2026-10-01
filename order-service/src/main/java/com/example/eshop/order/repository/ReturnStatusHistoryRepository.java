package com.example.eshop.order.repository;

import com.example.eshop.order.model.ReturnStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory, Long> {

    List<ReturnStatusHistory> findByReturnRequestOrderByChangedAtDesc(Long returnId);
}

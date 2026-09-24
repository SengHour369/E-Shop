package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.ReturnStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory, Long> {

    List<ReturnStatusHistory> findByReturnRequestOrderByChangedAtDesc(Long returnId);
}
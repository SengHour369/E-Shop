package com.example.eshop.admin.repository;
import com.example.eshop.admin.model.LiveTestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LiveTestRunRepository extends JpaRepository<LiveTestRun,UUID> {
 List<LiveTestRun> findByStatus(String status);
 List<LiveTestRun> findTop20ByActorIdOrderByStartedAtDesc(long actorId);
}

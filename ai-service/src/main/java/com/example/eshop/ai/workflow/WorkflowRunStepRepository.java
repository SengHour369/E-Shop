package com.example.eshop.ai.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface WorkflowRunStepRepository extends JpaRepository<WorkflowRunStep,UUID> {
 List<WorkflowRunStep> findByRunIdOrderByPositionAsc(UUID runId);
}

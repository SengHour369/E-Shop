package com.example.eshop.ai.workflow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.Instant;
public interface WorkflowTriggerJobRepository extends JpaRepository<WorkflowTriggerJob,UUID>{
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select j from WorkflowTriggerJob j where j.status='QUEUED' order by j.createdAt") List<WorkflowTriggerJob> queued(Pageable page);
 List<WorkflowTriggerJob> findByStatusAndStartedAtBefore(String status,Instant before);
 List<WorkflowTriggerJob> findByTriggerIdAndStatus(UUID id,String status);
 Page<WorkflowTriggerJob> findByTriggerIdAndOwnerId(UUID id,long owner,Pageable page);
}

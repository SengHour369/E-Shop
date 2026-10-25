package com.example.eshop.ai.workflow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.time.Instant;
public interface WorkflowTriggerRepository extends JpaRepository<WorkflowTrigger,UUID>{
 Optional<WorkflowTrigger> findByWorkflowIdAndOwnerId(UUID id,long owner);
 List<WorkflowTrigger> findByStateAndAuthorizationExpiresAtBefore(String state,Instant expiry);
 List<WorkflowTrigger> findByOwnerIdOrderByActivatedAtDesc(long owner);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select t from WorkflowTrigger t where t.id=:id") Optional<WorkflowTrigger> locked(@Param("id") UUID id);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select t from WorkflowTrigger t where t.state='ACTIVE' and t.triggerType='SCHEDULED' and t.nextRunAt<=:now order by t.nextRunAt") List<WorkflowTrigger> due(@Param("now")Instant now,Pageable page);
 List<WorkflowTrigger> findByStateAndTriggerTypeAndEventType(String state,String triggerType,String eventType);
}

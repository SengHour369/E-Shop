package com.example.eshop.ai.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;

public interface WorkflowRunRepository
        extends JpaRepository<WorkflowRun, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<WorkflowRun> {

 long countByOwnerIdAndStartedAtAfter(
         long ownerId,
         java.time.Instant after
 );

 long countByOwnerIdAndStatusAndStartedAtAfter(
         long ownerId,
         String status,
         java.time.Instant after
 );

 @org.springframework.data.jpa.repository.Lock(
         jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
 )
 @org.springframework.data.jpa.repository.Query(
         "select r from WorkflowRun r "
                 + "where r.id=:id and r.ownerId=:owner"
 )
 Optional<WorkflowRun> lockOwned(
         @org.springframework.data.repository.query.Param("id") UUID id,
         @org.springframework.data.repository.query.Param("owner") long owner
 );

 Optional<WorkflowRun> findByIdAndOwnerId(UUID id, long ownerId);

 Page<WorkflowRun> findByOwnerId(long ownerId, Pageable pageable);
}
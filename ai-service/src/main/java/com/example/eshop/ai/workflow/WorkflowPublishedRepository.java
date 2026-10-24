package com.example.eshop.ai.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WorkflowPublishedRepository
        extends JpaRepository<WorkflowPublishedVersion, UUID> {

 @org.springframework.data.jpa.repository.Query(
         "select count(distinct v.workflowId) "
                 + "from WorkflowPublishedVersion v where v.ownerId=:owner"
 )
 long countPublishedWorkflows(
         @org.springframework.data.repository.query.Param("owner") long owner
 );

 Optional<WorkflowPublishedVersion> findByWorkflowIdAndDraftRevision(
         UUID workflowId,
         long draftRevision
 );

 Optional<WorkflowPublishedVersion> findByIdAndOwnerId(
         UUID id,
         long ownerId
 );

 List<WorkflowPublishedVersion> findByWorkflowIdAndOwnerIdOrderByDraftRevisionDesc(
         UUID workflowId,
         long ownerId
 );
}
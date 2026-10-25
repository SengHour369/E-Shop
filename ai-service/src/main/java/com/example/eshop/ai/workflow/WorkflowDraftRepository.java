package com.example.eshop.ai.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowDraftRepository extends JpaRepository<WorkflowDraft, UUID> {

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
    )
    @org.springframework.data.jpa.repository.Query(
            "select d from WorkflowDraft d "
                    + "where d.id=:id and d.ownerId=:owner"
    )
    Optional<WorkflowDraft> lockOwned(
            @org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("owner") long owner
    );

    Page<WorkflowDraft> findByOwnerIdAndNameContainingIgnoreCase(
            long ownerId,
            String name,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("""
            select d from WorkflowDraft d
            where d.ownerId=:owner
              and (:search='' or locate(lower(:search),lower(d.name))>0)
              and (:module='' or d.moduleCode=:module)
              and (:trigger='' or d.triggerType=:trigger)
              and (:status=''
                or (:status='PUBLISHED' and exists (select v.id from WorkflowPublishedVersion v where v.workflowId=d.id and v.draftRevision=d.revision))
                or (:status='DRAFT' and not exists (select v.id from WorkflowPublishedVersion v where v.workflowId=d.id and v.draftRevision=d.revision)))
            """)
    Page<WorkflowDraft> searchOwned(
            @org.springframework.data.repository.query.Param("owner") long owner,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("module") String module,
            @org.springframework.data.repository.query.Param("trigger") String trigger,
            @org.springframework.data.repository.query.Param("status") String status,
            Pageable page);

    long countByOwnerId(long ownerId);

    Optional<WorkflowDraft> findByIdAndOwnerId(UUID id, long ownerId);
}
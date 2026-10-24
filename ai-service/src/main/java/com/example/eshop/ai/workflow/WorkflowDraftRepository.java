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

    long countByOwnerId(long ownerId);

    Optional<WorkflowDraft> findByIdAndOwnerId(UUID id, long ownerId);
}
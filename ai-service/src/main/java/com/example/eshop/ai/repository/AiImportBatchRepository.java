package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportBatch;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportBatchRepository extends JpaRepository<AiImportBatch, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from AiImportBatch b where b.id = :id")
    Optional<AiImportBatch> lock(@Param("id") UUID id);
    Page<AiImportBatch> findByOwnerId(long ownerId, Pageable pageable);
    Page<AiImportBatch> findByAssignedReviewerIdAndStatus(long assignedReviewerId, String status, Pageable pageable);
    Page<AiImportBatch> findByAssignedReviewerId(long assignedReviewerId, Pageable pageable);
    Page<AiImportBatch> findByStatus(String status, Pageable pageable);
}

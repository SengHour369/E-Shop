package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportRow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportRowRepository extends JpaRepository<AiImportRow, UUID> {
    @Query("select r from AiImportRow r where r.batchId = :batchId and (:status is null or r.status = :status or (:status = 'EDITED' and r.edited = true)) order by r.rowNumber, r.id")
    Page<AiImportRow> page(@Param("batchId") UUID batchId, @Param("status") String status, Pageable pageable);
    List<AiImportRow> findByBatchId(UUID batchId);
    interface StatusSummary {
        String getStatus();
        long getRowCount();
        long getEditedCount();
    }
    @Query("select r.status as status, count(r) as rowCount, sum(case when r.edited = true then 1 else 0 end) as editedCount from AiImportRow r where r.batchId = :batchId group by r.status")
    List<StatusSummary> summarize(@Param("batchId") UUID batchId);
}

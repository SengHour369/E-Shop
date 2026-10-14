package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportAuditHistory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportAuditHistoryRepository extends JpaRepository<AiImportAuditHistory, UUID> {
    List<AiImportAuditHistory> findByBatchId(UUID batchId);
}

package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportApproval;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportApprovalRepository extends JpaRepository<AiImportApproval, UUID> {
    List<AiImportApproval> findByBatchId(UUID batchId);
}

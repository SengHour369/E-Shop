package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportRowExecution;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportRowExecutionRepository extends JpaRepository<AiImportRowExecution, UUID> {
    List<AiImportRowExecution> findByBatchId(UUID batchId);
}

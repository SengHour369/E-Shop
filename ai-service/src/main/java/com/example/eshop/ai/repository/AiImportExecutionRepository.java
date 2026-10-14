package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportExecution;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportExecutionRepository extends JpaRepository<AiImportExecution, UUID> {
    List<AiImportExecution> findByBatchId(UUID batchId);
}

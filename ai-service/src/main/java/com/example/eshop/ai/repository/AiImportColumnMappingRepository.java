package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportColumnMapping;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportColumnMappingRepository extends JpaRepository<AiImportColumnMapping, UUID> {
    List<AiImportColumnMapping> findByBatchId(UUID batchId);
}

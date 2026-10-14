package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportRevision;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportRevisionRepository extends JpaRepository<AiImportRevision, UUID> {
    List<AiImportRevision> findByBatchId(UUID batchId);
}

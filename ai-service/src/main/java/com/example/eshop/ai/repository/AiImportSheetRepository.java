package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportSheet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportSheetRepository extends JpaRepository<AiImportSheet, UUID> {
    List<AiImportSheet> findByBatchId(UUID batchId);
}

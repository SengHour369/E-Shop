package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportFile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportFileRepository extends JpaRepository<AiImportFile, UUID> {
    List<AiImportFile> findByBatchId(UUID batchId);
}

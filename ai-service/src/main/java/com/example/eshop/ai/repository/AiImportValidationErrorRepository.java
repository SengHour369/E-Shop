package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportValidationError;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AiImportValidationErrorRepository extends JpaRepository<AiImportValidationError, UUID> {
    List<AiImportValidationError> findByBatchId(UUID batchId);
    void deleteByBatchId(UUID batchId);
}

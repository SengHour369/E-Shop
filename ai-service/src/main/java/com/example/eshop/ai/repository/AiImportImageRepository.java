package com.example.eshop.ai.repository;
import com.example.eshop.ai.model.AiImportImage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface AiImportImageRepository extends JpaRepository<AiImportImage,UUID>{
 interface Metadata{UUID getId();UUID getRowId();String getChecksum();String getFilename();int getSizeBytes();}
 @Query("select i.id as id,i.rowId as rowId,i.checksum as checksum,i.filename as filename,i.sizeBytes as sizeBytes from AiImportImage i where i.batchId=:batch order by i.createdAt")
 List<Metadata> metadata(@Param("batch")UUID batch);
 @Query("select coalesce(sum(i.sizeBytes),0) from AiImportImage i where i.batchId=:batch") long bytes(@Param("batch")UUID batch);
 long countByBatchId(UUID batchId);
}

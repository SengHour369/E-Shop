package com.example.eshop.ai.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="ai_import_image") @Getter @Setter
public class AiImportImage {
 @Id private UUID id=UUID.randomUUID();@Column(nullable=false) private UUID batchId;@Column(nullable=false) private UUID rowId;
 @Column(nullable=false) private long actorId;@Column(nullable=false,length=64) private String checksum;
 @Column(nullable=false) private String filename;@Column(nullable=false) private int sizeBytes;
 @JsonIgnore @Column(nullable=false,columnDefinition="bytea") private byte[] content;
 @Column(nullable=false) private Instant createdAt=Instant.now();
}

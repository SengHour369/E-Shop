package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_import_batch") @Getter @Setter
public class AiImportBatch {
    @Id private UUID id = UUID.randomUUID();
    @Version private long lockVersion;
@Column(nullable=false) private long ownerId;
@Column(nullable=false) private int revision = 1;
@Column(nullable=false) private String status = "DRAFT";
@Column(nullable=false, unique=true) private String checksum;
private Integer validatedRevision;
private Integer approvedRevision;
private Long reviewerId;
private Long assignedReviewerId;
private Long lastEditorId;
private String requestId;
private String traceId;
private Instant createdAt = Instant.now();
}

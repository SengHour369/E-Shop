package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_import_audit_history") @Getter @Setter
public class AiImportAuditHistory {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable=false) private UUID batchId;
private int revision;
private long actorId;
private String action;
private String requestId;
private String traceId;
@Column(columnDefinition="text") private String details;
private Instant createdAt = Instant.now();
}

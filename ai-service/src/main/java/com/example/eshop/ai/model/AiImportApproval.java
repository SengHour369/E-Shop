package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_import_approval") @Getter @Setter
public class AiImportApproval {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable=false) private UUID batchId;
private int revision;
private long actorId;
private String decision;
@Column(columnDefinition="text") private String reason;
private Instant createdAt = Instant.now();
}

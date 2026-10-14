package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_import_revision") @Getter @Setter
public class AiImportRevision {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable=false) private UUID batchId;
private int revision;
@Column(nullable=false, columnDefinition="text") private String snapshot;
private Instant createdAt = Instant.now();
}

package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_import_execution") @Getter @Setter
public class AiImportExecution {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable=false) private UUID batchId;
private int revision;
private String status = "PROCESSING";
private Instant startedAt = Instant.now();
private Instant completedAt;
}

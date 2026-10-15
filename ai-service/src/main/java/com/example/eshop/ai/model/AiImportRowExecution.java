package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_import_row_execution")
@Getter
@Setter
public class AiImportRowExecution {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID batchId;

    private UUID executionId;

    private UUID rowId;

    private String idempotencyKey;

    private String status;

    private String resourceId;

    private String errorCode;

    private Instant createdAt = Instant.now();
}
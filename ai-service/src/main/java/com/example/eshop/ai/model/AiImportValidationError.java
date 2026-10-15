package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_import_validation_error")
@Getter
@Setter
public class AiImportValidationError {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private UUID rowId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(columnDefinition = "text")
    private String explanation;
}
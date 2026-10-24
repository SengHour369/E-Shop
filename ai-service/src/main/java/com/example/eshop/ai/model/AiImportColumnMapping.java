package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_import_column_mapping")
@Getter
@Setter
public class AiImportColumnMapping {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private UUID sheetId;

    @Column(nullable = false, columnDefinition = "text")
    private String suggestions = "{}";

    @Column(nullable = false, columnDefinition = "text")
    private String confirmedValues = "{}";
}
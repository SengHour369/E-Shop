package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_import_row")
@Getter
@Setter
public class AiImportRow {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private UUID sheetId;

    private int rowNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String originalValues;

    @Column(nullable = false, columnDefinition = "text")
    private String valuesJson = "{}";

    @Column(columnDefinition="text") private String currentValues;

    private String status = "UNVALIDATED";

    private boolean edited;

    private boolean skipped;

    private String resourceId;

    private String affectedResourceId;
}
package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_import_sheet")
@Getter
@Setter
public class AiImportSheet {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private String name;

    private String tool;

    @Column(nullable = false, columnDefinition = "text")
    private String headers;

    private boolean confirmed;
}
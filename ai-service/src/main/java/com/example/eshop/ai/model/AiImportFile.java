package com.example.eshop.ai.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_import_file")
@Getter
@Setter
public class AiImportFile {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID batchId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;
}
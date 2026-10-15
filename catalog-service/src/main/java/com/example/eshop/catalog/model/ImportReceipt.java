package com.example.eshop.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "ai_import_receipt")
@Getter
@Setter
public class ImportReceipt {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false)
    private long actorId;

    @Column(nullable = false)
    private String checksum;

    private String resourceId;

    private Instant createdAt;
}
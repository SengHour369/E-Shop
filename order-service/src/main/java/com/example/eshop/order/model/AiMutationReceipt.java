package com.example.eshop.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "ai_mutation_receipts") @Getter @Setter
public class AiMutationReceipt {
    @Id @Column(length = 120) private String id;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Column(nullable = false, columnDefinition = "TEXT") private String responseJson;
}

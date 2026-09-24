package com.example.learning_spring_security.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_sku_id", nullable = false)
    private ProductSku productSku;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long reservedQuantity;

    @Column(nullable = false)
    private Long availableQuantity;

    private String warehouseLocation;

    private LocalDateTime lastRestockedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.reservedQuantity == null) {
            this.reservedQuantity = 0L;
        }

        if (this.availableQuantity == null) {
            this.availableQuantity = this.quantity;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        this.availableQuantity = this.quantity - this.reservedQuantity;
    }
}
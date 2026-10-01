package com.example.eshop.catalog.model;

import com.example.eshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Getter @Setter
@Table(name = "promotion_skus",
    uniqueConstraints = @UniqueConstraint(name = "uk_promotion_sku", columnNames = {"promotion_id", "product_sku_id"}),
    indexes = @Index(name = "ix_promotion_sku_lookup", columnList = "product_sku_id,promotion_id"))
public class PromotionSku extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "promotion_id") private Promotion promotion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_sku_id") private ProductSku productSku;
}


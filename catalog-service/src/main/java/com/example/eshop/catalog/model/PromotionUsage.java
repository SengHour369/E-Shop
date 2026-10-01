package com.example.eshop.catalog.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Getter @Setter
@Table(name = "promotion_usages", uniqueConstraints =
    @UniqueConstraint(name = "uk_promotion_order", columnNames = {"promotion_id", "order_id"}),
    indexes = {
        @Index(name = "ix_usage_customer", columnList = "promotion_id,user_id,released"),
        @Index(name = "ix_usage_order", columnList = "order_id")
    })
public class PromotionUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "promotion_id") private Promotion promotion;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Long orderId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal discountAmount;
    @Column(nullable = false) private LocalDateTime usedAt;
    @Column(nullable = false) private boolean released;
}


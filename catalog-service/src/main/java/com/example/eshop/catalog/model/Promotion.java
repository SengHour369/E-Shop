package com.example.eshop.catalog.model;

import com.example.eshop.common.entity.BaseEntity;
import com.example.eshop.catalog.enumeration.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "promotions", indexes = {
    @Index(name = "ix_promotion_window", columnList = "status,is_active,start_at,end_at")
})
@Getter @Setter
public class Promotion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(unique = true, length = 60) private String code;
    @Column(length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PromotionType promotionType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DiscountType discountType;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal discountValue;
    @Column(precision = 19, scale = 2) private BigDecimal maxDiscountAmount;
    @Column(precision = 19, scale = 2) private BigDecimal minimumOrderAmount;
    @Column(nullable = false) private LocalDateTime startAt;
    @Column(nullable = false) private LocalDateTime endAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PromotionStatus status = PromotionStatus.DRAFT;
    @Column(nullable = false) private int priority;
    private Long usageLimit;
    private Long usagePerCustomer;
    @Column(nullable = false) private boolean stackable;
    @Column(nullable = false) private boolean isActive = true;
    private String createdBy;
    private String updatedBy;
}


package com.example.eshop.catalog.dto.response;

import com.example.eshop.catalog.enumeration.*;
import com.example.eshop.catalog.model.Promotion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(Long id, String name, String code, String description,
    PromotionType promotionType, DiscountType discountType, BigDecimal discountValue,
    BigDecimal maxDiscountAmount, BigDecimal minimumOrderAmount, LocalDateTime startAt,
    LocalDateTime endAt, PromotionStatus status, int priority, Long usageLimit,
    Long usagePerCustomer, boolean stackable, boolean isActive,
    LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy, String updatedBy) {
    public static PromotionResponse from(Promotion p) {
        return new PromotionResponse(p.getId(), p.getName(), p.getCode(), p.getDescription(),
            p.getPromotionType(), p.getDiscountType(), p.getDiscountValue(), p.getMaxDiscountAmount(),
            p.getMinimumOrderAmount(), p.getStartAt(), p.getEndAt(), p.getStatus(), p.getPriority(),
            p.getUsageLimit(), p.getUsagePerCustomer(), p.isStackable(), p.isActive(),
            p.getCreatedAt(), p.getUpdatedAt(), p.getCreatedBy(), p.getUpdatedBy());
    }
}


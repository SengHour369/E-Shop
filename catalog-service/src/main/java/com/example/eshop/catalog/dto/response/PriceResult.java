package com.example.eshop.catalog.dto.response;
import com.example.eshop.catalog.enumeration.PromotionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PriceResult(Long productSkuId, BigDecimal originalPrice, BigDecimal finalPrice,
    BigDecimal discountAmount, BigDecimal discountPercentage, Long promotionId,
    String promotionName, PromotionType promotionType, boolean promotionApplied,
    LocalDateTime promotionStartAt, LocalDateTime promotionEndAt) {}


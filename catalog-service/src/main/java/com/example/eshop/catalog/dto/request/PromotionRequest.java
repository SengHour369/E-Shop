package com.example.eshop.catalog.dto.request;

import com.example.eshop.catalog.enumeration.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 60) String code,
    @Size(max = 2000) String description,
    @NotNull PromotionType promotionType,
    @NotNull DiscountType discountType,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) BigDecimal discountValue,
    @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal maxDiscountAmount,
    @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal minimumOrderAmount,
    @NotNull LocalDateTime startAt,
    @NotNull LocalDateTime endAt,
    int priority,
    @Min(0) Long usageLimit,
    @Min(0) Long usagePerCustomer,
    boolean stackable
) {}


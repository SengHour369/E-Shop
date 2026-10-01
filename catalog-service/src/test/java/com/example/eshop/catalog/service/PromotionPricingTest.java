package com.example.eshop.catalog.service;

import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.enumeration.*;
import com.example.eshop.catalog.service.impl.PromotionPricingService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class PromotionPricingTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
    private ProductSku sku(String price) {
        ProductSku s = new ProductSku(); s.setId(1L); s.setPrice(new BigDecimal(price)); return s;
    }
    private Promotion promotion(long id, DiscountType type, String value) {
        Promotion p = new Promotion(); p.setId(id); p.setName("Sale"); p.setPromotionType(PromotionType.FLASH_SALE);
        p.setStatus(PromotionStatus.ACTIVE); p.setDiscountType(type); p.setDiscountValue(new BigDecimal(value));
        p.setStartAt(now); p.setEndAt(now.plusHours(1)); return p;
    }
    @Test void percentageAndFixedAreCalculatedWithoutChangingBasePrice() {
        ProductSku sku = sku("100");
        var percentage = PromotionPricingService.select(sku, List.of(promotion(1, DiscountType.PERCENTAGE, "20")), now, null, p -> true);
        assertThat(percentage.finalPrice()).isEqualByComparingTo("80.00");
        assertThat(percentage.discountPercentage()).isEqualByComparingTo("20.00");
        assertThat(sku.getPrice()).isEqualByComparingTo("100");
        var fixed = PromotionPricingService.select(sku, List.of(promotion(1, DiscountType.FIXED_AMOUNT, "25")), now, null, p -> true);
        assertThat(fixed.finalPrice()).isEqualByComparingTo("75.00");
    }
    @Test void capFloorAndRounding() {
        Promotion p = promotion(1, DiscountType.PERCENTAGE, "50");
        p.setMaxDiscountAmount(new BigDecimal("10"));
        assertThat(PromotionPricingService.select(sku("100"), List.of(p), now, null, x -> true).finalPrice()).isEqualByComparingTo("90");
        p.setDiscountType(DiscountType.FIXED_AMOUNT); p.setDiscountValue(new BigDecimal("200")); p.setMaxDiscountAmount(null);
        assertThat(PromotionPricingService.select(sku("100"), List.of(p), now, null, x -> true).finalPrice()).isZero();
        p.setDiscountType(DiscountType.PERCENTAGE); p.setDiscountValue(new BigDecimal("10"));
        assertThat(PromotionPricingService.select(sku("0.05"), List.of(p), now, null, x -> true).discountAmount()).isEqualByComparingTo("0.01");
        assertThat(PromotionPricingService.select(sku("0"), List.of(p), now, null, x -> true).discountPercentage()).isZero();
    }
    @Test void exactStartIncludedEndExcludedAndLateSchedulerSupported() {
        Promotion p = promotion(1, DiscountType.PERCENTAGE, "20"); p.setStatus(PromotionStatus.SCHEDULED);
        assertThat(PromotionPricingService.effective(p, now.minusNanos(1))).isFalse();
        assertThat(PromotionPricingService.effective(p, now)).isTrue();
        assertThat(PromotionPricingService.effective(p, p.getEndAt())).isFalse();
        p.setStatus(PromotionStatus.DISABLED); assertThat(PromotionPricingService.effective(p, now)).isFalse();
        p.setStatus(PromotionStatus.ACTIVE); p.setActive(false); assertThat(PromotionPricingService.effective(p, now)).isFalse();
    }
    @Test void cheapestThenHigherPriorityThenLowestIdRegardlessOfInputOrder() {
        Promotion a = promotion(3, DiscountType.PERCENTAGE, "20");
        Promotion b = promotion(2, DiscountType.FIXED_AMOUNT, "20");
        b.setPriority(1);
        assertThat(PromotionPricingService.select(sku("100"), List.of(a,b), now, null, p -> true).promotionId()).isEqualTo(2);
        a.setPriority(1);
        assertThat(PromotionPricingService.select(sku("100"), List.of(b,a), now, null, p -> true).promotionId()).isEqualTo(2);
        a.setDiscountValue(new BigDecimal("30"));
        assertThat(PromotionPricingService.select(sku("100"), List.of(b,a), now, null, p -> true).promotionId()).isEqualTo(3);
    }
    @Test void noPromotionMinimumBasketAndExhaustedAllowance() {
        ProductSku sku = sku("100");
        assertThat(PromotionPricingService.select(sku, List.of(), now, null, p -> true).finalPrice()).isEqualByComparingTo("100");
        Promotion p = promotion(1, DiscountType.PERCENTAGE, "20"); p.setMinimumOrderAmount(new BigDecimal("200"));
        assertThat(PromotionPricingService.select(sku, List.of(p), now, null, x -> true).promotionApplied()).isFalse();
        assertThat(PromotionPricingService.select(sku, List.of(p), now, new BigDecimal("200"), x -> true).promotionApplied()).isTrue();
        assertThat(PromotionPricingService.select(sku, List.of(p), now, new BigDecimal("200"), x -> false).promotionApplied()).isFalse();
    }
}


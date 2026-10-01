package com.example.eshop.catalog.service.impl;

import com.example.eshop.catalog.dto.response.PriceResult;
import com.example.eshop.catalog.enumeration.*;
import com.example.eshop.catalog.model.*;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.common.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PromotionPricingService {
    private final PromotionSkuRepository assignments;
    private final ProductSkuRepository skus;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PriceResult calculatePrice(Long skuId) {
        ProductSku sku = skus.findById(skuId).orElseThrow(() -> new ResourceNotFoundException("SKU not found"));
        return calculatePrices(List.of(sku)).get(skuId);
    }

    @Transactional(readOnly = true)
    public Map<Long, PriceResult> calculatePrices(List<ProductSku> skuList) {
        if (skuList.isEmpty()) return Map.of();
        LocalDateTime now = LocalDateTime.now(clock);
        var bySku = assignments.eligible(skuList.stream().map(ProductSku::getId).toList(), now).stream()
            .collect(Collectors.groupingBy(a -> a.getProductSku().getId(),
                Collectors.mapping(PromotionSku::getPromotion, Collectors.toList())));
        Map<Long, PriceResult> results = new HashMap<>();
        for (ProductSku sku : skuList) {
            boolean sellable = Boolean.TRUE.equals(sku.getProduct().getIsActive()) && !Boolean.TRUE.equals(sku.getProduct().getDeleted());
            results.put(sku.getId(), select(sku, sellable ? bySku.getOrDefault(sku.getId(), List.of()) : List.of(),
                now, null, p -> !Long.valueOf(0).equals(p.getUsagePerCustomer())));
        }
        return results;
    }

    // A scheduled promotion has effective ACTIVE status inside its window, even if the job is late.
    public static boolean effective(Promotion p, LocalDateTime now) {
        return p.isActive() && (p.getStatus() == PromotionStatus.ACTIVE || p.getStatus() == PromotionStatus.SCHEDULED)
            && !now.isBefore(p.getStartAt()) && now.isBefore(p.getEndAt());
    }

    public static PriceResult select(ProductSku sku, List<Promotion> candidates, LocalDateTime now,
                                     BigDecimal subtotal, Predicate<Promotion> usageEligible) {
        BigDecimal base = money(sku.getPrice());
        if (base.signum() < 0) throw new BusinessLogicException("SKU price cannot be negative");
        Promotion best = null;
        BigDecimal bestPrice = base;
        for (Promotion p : candidates) {
            if (!effective(p, now) || !usageEligible.test(p)) continue;
            if (p.getMinimumOrderAmount() != null && p.getMinimumOrderAmount().signum() > 0
                    && (subtotal == null || subtotal.compareTo(p.getMinimumOrderAmount()) < 0)) continue;
            BigDecimal discount = p.getDiscountType() == DiscountType.PERCENTAGE
                ? base.multiply(p.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : money(p.getDiscountValue());
            if (p.getMaxDiscountAmount() != null) discount = discount.min(p.getMaxDiscountAmount());
            BigDecimal price = money(base.subtract(discount).max(BigDecimal.ZERO));
            int comparison = price.compareTo(bestPrice);
            if (comparison < 0 || (comparison == 0 && best != null &&
                    (p.getPriority() > best.getPriority() ||
                     (p.getPriority() == best.getPriority() && p.getId() < best.getId())))) {
                best = p; bestPrice = price;
            }
        }
        BigDecimal discount = base.subtract(bestPrice);
        BigDecimal percentage = base.signum() == 0 ? money(BigDecimal.ZERO)
            : discount.multiply(new BigDecimal("100")).divide(base, 2, RoundingMode.HALF_UP);
        return new PriceResult(sku.getId(), base, bestPrice, discount, percentage,
            best == null ? null : best.getId(), best == null ? null : best.getName(),
            best == null ? null : best.getPromotionType(), best != null,
            best == null ? null : best.getStartAt(), best == null ? null : best.getEndAt());
    }

    public static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new BusinessLogicException("SKU price is missing");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}


package com.example.eshop.common.dto;
import java.math.BigDecimal;
import java.util.List;
public record CheckoutResult(Long orderId, String status, List<Line> items, BigDecimal total) {
    public record Line(Long productSkuId, Long quantity, BigDecimal originalPrice,
        BigDecimal discountAmount, BigDecimal finalPrice, Long promotionId, String promotionName) {}
}


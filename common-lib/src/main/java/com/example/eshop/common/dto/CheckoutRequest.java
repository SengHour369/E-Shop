package com.example.eshop.common.dto;
import java.util.List;
public record CheckoutRequest(Long orderId, Long userId, List<Line> items) {
    public record Line(Long productSkuId, Long quantity) {}
}


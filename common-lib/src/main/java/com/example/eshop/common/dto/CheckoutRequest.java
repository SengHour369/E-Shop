package com.example.eshop.common.dto;
import java.util.List;
import java.util.UUID;
public record CheckoutRequest(Long orderId, Long userId, List<Line> items, UUID guestSessionId) {
    public CheckoutRequest(Long orderId,Long userId,List<Line> items) { this(orderId,userId,items,null); }
    public record Line(Long productSkuId, Long quantity) {}
}

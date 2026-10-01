package com.example.eshop.order.mapper;

import com.example.eshop.order.model.OrderDetail;
import com.example.eshop.order.model.OrderItem;
import com.example.eshop.order.dto.response.OrderItemResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderItemMapper {

    public static OrderItem toEntity(OrderDetail order, Long productSkuId, Long quantity, BigDecimal unitPrice) {
        if (order == null || productSkuId == null || quantity == null) {
            throw new IllegalArgumentException("Order, productSkuId and quantity cannot be null");
        }

        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));

        return OrderItem.builder()
                .orderDetail(order)
                .productSkuId(productSkuId)
                .quantity(quantity)
                .unitPrice(price)
                .totalPrice(totalPrice)
                .build();
    }

    public OrderItemResponse toResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productSkuId(orderItem.getProductSkuId())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .baseUnitPrice(orderItem.getBaseUnitPrice())
                .discountAmount(orderItem.getDiscountAmount())
                .finalUnitPrice(orderItem.getFinalUnitPrice())
                .promotionId(orderItem.getPromotionId())
                .promotionName(orderItem.getPromotionName())
                .totalPrice(orderItem.getTotalPrice())
                .createdAt(orderItem.getCreatedAt())
                .updatedAt(orderItem.getUpdatedAt())
                .build();
    }

    public static void updateEntity(OrderItem orderItem, Long quantity) {
        if (orderItem == null || quantity == null) {
            return;
        }

        orderItem.setQuantity(quantity);

        if (orderItem.getUnitPrice() != null) {
            BigDecimal newTotalPrice = orderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(quantity));
            orderItem.setTotalPrice(newTotalPrice);
        }
    }
}

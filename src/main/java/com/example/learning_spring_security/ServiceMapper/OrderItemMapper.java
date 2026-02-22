package com.example.learning_spring_security.ServiceMapper;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.OrderDetail;
import com.example.learning_spring_security.Model.OrderItem;
import com.example.learning_spring_security.Model.ProductSku;
import com.example.learning_spring_security.dto.Response.OrderItemResponse;
import com.example.learning_spring_security.dto.Response.ResponseErrorTemplate;

import java.math.BigDecimal;

public class OrderItemMapper {

    public static OrderItem toEntity(OrderDetail order, ProductSku productSku, Long quantity) {
        if (order == null || productSku == null || quantity == null) {
            throw new IllegalArgumentException("Order, ProductSku and quantity cannot be null");
        }

        BigDecimal unitPrice = productSku.getPrice();
        if (unitPrice == null) {
            throw new IllegalArgumentException("Product SKU price cannot be null");
        }

        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        return OrderItem.builder()
                .orderDetail(order)
                .productSku(productSku)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();
    }

    public static OrderItemResponse toResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        OrderItemResponse.OrderItemResponseBuilder builder = OrderItemResponse.builder()
                .id(orderItem.getId())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice());

        if (orderItem.getProductSku() != null) {
            builder.productSku(ProductSkuMapper.toResponse(orderItem.getProductSku()));
        }

        return   builder.build();

    }

    public static void updateEntity(OrderItem orderItem, Long quantity) {
        if (orderItem == null || quantity == null) {
            return;
        }

        orderItem.setQuantity(quantity);

        // Recalculate total price
        if (orderItem.getUnitPrice() != null) {
            BigDecimal newTotalPrice = orderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(quantity));
            orderItem.setTotalPrice(newTotalPrice);
        }

    }
}
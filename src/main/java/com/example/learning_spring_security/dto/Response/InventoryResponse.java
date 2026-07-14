package com.example.learning_spring_security.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryResponse {

    private Long id;

    private Long productSkuId;
    private String sku;
    private String productName;

    private Long quantity;
    private Long reservedQuantity;
    private Long availableQuantity;

    private String warehouseLocation;

    private LocalDateTime lastRestockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer lowStockThreshold;
}
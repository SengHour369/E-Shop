package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private Long id;

    @JsonProperty("product_sku_id")
    private Long productSkuId;

    private String sku;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("product_name")
    private String productName;

    private String description;

    @JsonProperty("stock_qty")
    private Long quantity;

    @JsonProperty("reserved_qty")
    private Long reservedQuantity;

    @JsonProperty("available_qty")
    private Long availableQuantity;

    @JsonProperty("warehouse_location")
    private String warehouseLocation;

    @JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold;

    @JsonProperty("stock_status")
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK

    @JsonProperty("last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
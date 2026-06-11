package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class InventoryRequest {

    @NotNull(message = "Product SKU ID is required")
    @JsonProperty("product_sku_id")
    private Long productSkuId;

    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Long quantity;

    @JsonProperty("warehouse_location")
    private String warehouseLocation;
}
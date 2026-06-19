package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class ProductSkuResponse {
    private Long id;
    private String sku;
    private String description;
    private BigDecimal price;
    private Long quantity;
    @com.fasterxml.jackson.annotation.JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold;
    @com.fasterxml.jackson.annotation.JsonProperty("is_default")
    private Boolean isDefault;

    // Optional: attributes assigned to this SKU
    @JsonProperty("attributes")
    private List<ProductAttributeResponse> ProductAttributeResponse;
}
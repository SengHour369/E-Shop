package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

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

    @JsonProperty("compare_price")
    private BigDecimal comparePrice;

    private Long quantity;

    @JsonProperty("low_stock_threshold")
    private Integer lowStockThreshold;

    private String color;
    private String size;
    private String material;

    @JsonProperty("is_default")
    private Boolean isDefault;

    @JsonProperty("product_id")
    private Long productId;  // បន្ថែម Product ID
}
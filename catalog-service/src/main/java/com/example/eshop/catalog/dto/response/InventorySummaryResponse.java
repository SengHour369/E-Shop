package com.example.eshop.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryResponse {
    @JsonProperty("total_products")
    private Long totalProducts;

    @JsonProperty("total_stock")
    private Long totalStock;

    @JsonProperty("low_stock")
    private Long lowStock;

    @JsonProperty("out_of_stock")
    private Long outOfStock;
}
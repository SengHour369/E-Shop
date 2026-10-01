package com.example.eshop.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CartRequest {

    @NotNull(message = "Product  ID is required")
    @JsonProperty("product_id")
    private Long productSkuId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Long quantity;

    // Retained for request compatibility only. Catalog pricing always overrides this value.
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
}

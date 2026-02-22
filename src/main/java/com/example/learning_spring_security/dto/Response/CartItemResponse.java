package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CartItemResponse {
    private Long id;

    @JsonProperty("product_sku_id")
    private Long productSkuId;

    @JsonProperty("product_sku")
    private ProductSkuResponse productSku;

    private Long quantity;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;  
}
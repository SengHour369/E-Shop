package com.example.eshop.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CartItemResponse {

    private Long id;

    // Was a nested ProductSkuResponse/name/image from catalog-service.
    // TODO: cross-service call via Feign to catalog-service to enrich SKU name/image/price.
    @JsonProperty("product_sku_id")
    private Long productSkuId;

    private Long quantity;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}

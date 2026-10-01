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
public class OrderItemResponse {
    private Long id;

    // Was a nested ProductSkuResponse/productName from catalog-service.
    // TODO: cross-service call via Feign to catalog-service to enrich SKU/product name.
    @JsonProperty("product_sku_id")
    private Long productSkuId;

    private Long quantity;
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
    private BigDecimal baseUnitPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalUnitPrice;
    private Long promotionId;
    private String promotionName;
    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}

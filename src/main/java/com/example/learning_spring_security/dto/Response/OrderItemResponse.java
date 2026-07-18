package com.example.learning_spring_security.dto.Response;

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
    @JsonProperty("product_name")
    private String productName;
    private Long quantity;
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    @JsonProperty("product_sku")
    private ProductSkuResponse productSku;
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
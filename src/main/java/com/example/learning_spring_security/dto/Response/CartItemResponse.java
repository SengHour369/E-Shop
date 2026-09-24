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
public class CartItemResponse {

//    @JsonProperty("product_sku_id")
//    private Long productSkuId;

    private Long id;
    private ProductSkuResponse productSku;
    private String image;
    private String name;

    private Long quantity;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
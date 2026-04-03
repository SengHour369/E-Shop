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

//    @JsonProperty("product_sku_id")
//    private Long productSkuId;

    @JsonProperty("product_sku")
    private Long id;
    private ProductSkuResponse productSku;
    private String image;
    private String name;

    private Long quantity;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;  
}
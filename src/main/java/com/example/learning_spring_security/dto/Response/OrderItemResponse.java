package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class OrderItemResponse {
    private Long id;
    private Long quantity;

    @JsonProperty("unit_price")
    private BigDecimal unitPrice;  // បន្ថែមតម្លៃក្នុងមួយឯកតា

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("product_sku")
    private ProductSkuResponse productSku;
}
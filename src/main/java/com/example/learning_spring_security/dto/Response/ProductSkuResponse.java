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
    private String sku;
    private String description;
    private BigDecimal price;
    private Long quantity;
}
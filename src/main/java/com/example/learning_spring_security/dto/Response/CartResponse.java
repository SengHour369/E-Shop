package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CartResponse {
    private Long id;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("total_items")
    private Integer totalItems;

    private List<CartItemResponse> items;

}
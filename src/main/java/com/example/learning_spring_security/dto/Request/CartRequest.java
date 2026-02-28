package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CartRequest {

    @NotNull(message = "Product  ID is required")
    @JsonProperty("product_id")
    private Long productId;
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Long quantity;
}
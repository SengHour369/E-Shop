package com.example.eshop.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class RestockRequest {

    @NotNull(message = "Quantity to add is required")
    @Positive(message = "Quantity must be positive")
    private Long quantity;
}
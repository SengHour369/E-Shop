package com.example.learning_spring_security.dto.Request;

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
package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class CreateReturnRequest {

    @NotNull(message = "Order id is required")
    @JsonProperty("order_id")
    private Long orderId;

    @NotNull(message = "Customer id is required")
    @JsonProperty("customer_id")
    private Long customerId;

    @NotNull(message = "Product id is required")
    @JsonProperty("product_id")
    private Long productId;

    @NotBlank(message = "Return type is required")
    @JsonProperty("return_type")
    private String returnType;

    private String reason;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
}
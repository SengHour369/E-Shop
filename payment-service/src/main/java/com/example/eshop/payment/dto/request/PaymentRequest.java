package com.example.eshop.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class PaymentRequest {

    @NotNull(message = "Payment method is required")
    @JsonProperty("payment_method")
    private String paymentMethod;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("payment_provider")
    private String paymentProvider;

    // Cross-service: the monolith derived this from orderDetail.getUser().getId().
    // Since Order/User now live in other services, the caller (order-service, via Feign)
    // is expected to supply it directly.
    // TODO: cross-service call via Feign to order-service/auth-service if this is not supplied.
    @JsonProperty("user_id")
    private Long userId;

}

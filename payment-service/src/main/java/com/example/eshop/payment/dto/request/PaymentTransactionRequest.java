package com.example.eshop.payment.dto.request;

import com.example.eshop.payment.enumeration.PaymentMethod;
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
public class PaymentTransactionRequest {

    @NotNull(message = "Order id is required")
    @JsonProperty("order_id")
    private Long orderId;

    @NotNull(message = "Customer id is required")
    @JsonProperty("customer_id")
    private Long customerId;

    @NotNull(message = "Payment method is required")
    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    @JsonProperty("masked_account")
    private String maskedAccount;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private String currency;

    private String remarks;
}

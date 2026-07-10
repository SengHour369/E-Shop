package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class PaymentResponse {
    private Long id;

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("order_number")
    private String orderNumber;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("payment_date")
    private LocalDateTime paymentDate;

    private BigDecimal amount;
    private String currency;
    private String status;

    @JsonProperty("transaction_id")
    private String transactionId;

    private String code;

    private String codeOrder;

    @JsonProperty("payment_provider")
    private String paymentProvider;
}
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
public class PaymentTransactionResponse {

    private Long id;

    @JsonProperty("transaction_no")
    private String transactionNo;

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("masked_account")
    private String maskedAccount;

    private BigDecimal amount;

    private String currency;

    private String status;

    private String remarks;

    @JsonProperty("status_history")
    private List<PaymentTransactionStatusHistoryResponse> statusHistory;
}
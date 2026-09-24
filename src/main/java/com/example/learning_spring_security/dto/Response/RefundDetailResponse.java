package com.example.learning_spring_security.dto.Response;

import com.example.learning_spring_security.Enumeration.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDetailResponse {

    @JsonProperty("refund_id")
    private String refundId;

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("payment_transaction_id")
    private Long paymentTransactionId;

    @JsonProperty("transaction_no")
    private String transactionNo;

    @JsonProperty("payment_method")
    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    private String status;

    private String reason;

    private String remark;

    @JsonProperty("requested_at")
    private LocalDateTime requestedAt;

    @JsonProperty("requested_by")
    private String requestedBy;

    @JsonProperty("processed_at")
    private LocalDateTime processedAt;

    @JsonProperty("processed_by")
    private String processedBy;
}
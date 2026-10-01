package com.example.eshop.order.dto.response;

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

    // Was `customerName`/`customerEmail` (User join, auth-service) and `transactionNo`/`paymentMethod`
    // (PaymentTransaction join, payment-service).
    // TODO: cross-service calls via Feign to auth-service/payment-service to enrich these.
    @JsonProperty("payment_transaction_id")
    private Long paymentTransactionId;

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

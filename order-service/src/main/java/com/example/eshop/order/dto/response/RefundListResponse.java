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
public class RefundListResponse {

    @JsonProperty("refund_id")
    private String refundId;

    @JsonProperty("order_no")
    private String orderNo;

    // Was `customerName` (User join, auth-service).
    // TODO: cross-service call via Feign to auth-service to enrich customer name.
    @JsonProperty("requested_at")
    private LocalDateTime requestedAt;

    private BigDecimal amount;
    private String status;
}

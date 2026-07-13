package com.example.learning_spring_security.dto.Response;

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

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("requested_at")
    private LocalDateTime requestedAt;

    private BigDecimal amount;

    private String status;
}
package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundSummaryResponse {

    @JsonProperty("total_refunds")
    private Long totalRefunds;

    @JsonProperty("completed_refunds")
    private Long completedRefunds;

    @JsonProperty("pending_refunds")
    private Long pendingRefunds;

    @JsonProperty("refunded_amount")
    private BigDecimal refundedAmount;
}
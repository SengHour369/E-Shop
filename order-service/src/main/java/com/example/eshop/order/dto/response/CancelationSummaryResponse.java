package com.example.eshop.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelationSummaryResponse {

    private Long totalCancelations;
    private Long pendingReview;
    private Double cancelationRate;
    private BigDecimal valueLost;

    public CancelationSummaryResponse(Long totalCancelations, Long pendingReview, BigDecimal valueLost) {
        this.totalCancelations = totalCancelations;
        this.pendingReview = pendingReview;
        this.valueLost = valueLost;
    }
}

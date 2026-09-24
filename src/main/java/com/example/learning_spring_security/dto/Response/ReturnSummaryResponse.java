package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnSummaryResponse {

    @JsonProperty("total_returns")
    private Long totalReturns;

    @JsonProperty("processed_returns")
    private Long processedReturns;

    @JsonProperty("pending_returns")
    private Long pendingReturns;

    @JsonProperty("return_rate")
    private Double returnRate;

    public ReturnSummaryResponse(Long totalReturns, Long processedReturns, Long pendingReturns) {
        this.totalReturns = totalReturns;
        this.processedReturns = processedReturns;
        this.pendingReturns = pendingReturns;
    }
}
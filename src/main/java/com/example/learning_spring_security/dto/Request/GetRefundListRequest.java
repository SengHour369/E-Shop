package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRefundListRequest {

    // 1=refundId, 2=orderNo, 3=customerName, 4=status, 5=dateRange ("fromDate,toDate", ISO LocalDateTime)
    @JsonProperty("criteria_type")
    private Integer criteriaType;

    @JsonProperty("criteria_value")
    private String criteriaValue;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;
}
package com.example.eshop.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetReturnRequest {

    @JsonProperty("criteria_type")
    private Integer criteriaType;   // 1=returnId, 2=orderNo, 3=customerName, 4=productName, 5=status, 6=returnType

    @JsonProperty("criteria_value")
    private String criteriaValue;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;
}

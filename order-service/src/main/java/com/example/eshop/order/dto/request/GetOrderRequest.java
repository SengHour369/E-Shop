package com.example.eshop.order.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetOrderRequest {

    @JsonProperty("criteria_type")
    private Integer criteriaType;

    @JsonProperty("criteria_value")
    private String criteriaValue;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;
}

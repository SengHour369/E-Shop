package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetReturnRequest {

    @JsonProperty("criteria_type")
    private Integer criteriaType;   // e.g. 1=returnId, 2=orderNo, 3=customerName, 4=productName, 5=status, 6=returnType, 7=dateRange? etc.

    @JsonProperty("criteria_value")
    private String criteriaValue;   // the search term

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;
}
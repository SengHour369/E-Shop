package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetFunctionPermissionRequest {

    @JsonProperty("criteria_type")
    private Integer criteriaType;

    @JsonProperty("criteria_value")
    private String criteriaValue;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;
}
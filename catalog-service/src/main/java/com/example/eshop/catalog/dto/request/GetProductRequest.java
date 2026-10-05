package com.example.eshop.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetProductRequest {

    @JsonProperty("criteria_type")
    private Integer criteriaType;

    @JsonProperty("criteria_value")
    private String criteriaValue;

    @JsonProperty("is_active")
    private Boolean isActive;

    @Builder.Default
    @jakarta.validation.constraints.Min(1)
    private int page = 1;

    @Builder.Default
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(100)
    private int size = 10;
}

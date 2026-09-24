package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetUserGroupRequest {

    // criteriaType:
    // 0 or null = all active groups
    // 1 = by group code (exact)
    // 2 = by group name (fuzzy)
    // 3 = by isActive ("true" or "false")
    // 4 = by display (fuzzy)
    @JsonProperty("criteria_type")
    private Integer criteriaType;

    @JsonProperty("criteria_value")
    private String criteriaValue;

    private int page = 1;
    private int size = 10;
}
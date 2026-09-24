package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetUserPermissionRequest {

    // criteriaType:
    // 0 or null = all permissions
    // 1 = filter by userId
    // 2 = filter by funcId
    // 3 = filter by userId + funcId (criteriaValue = "userId:funcId")
    // 4 = filter by isActive (criteriaValue = "true" or "false")
    // 5 = filter by userId + isActive (criteriaValue = "userId:true/false")
    @JsonProperty("criteria_type")
    private Integer criteriaType;

    @JsonProperty("criteria_value")
    private String criteriaValue;

    private int page = 1;
    private int size = 10;
}
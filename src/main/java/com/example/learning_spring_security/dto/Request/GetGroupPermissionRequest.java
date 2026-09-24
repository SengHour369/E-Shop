package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetGroupPermissionRequest {

    // criteriaType:
    // 0 or null = all
    // 1 = by groupId
    // 2 = by funcId
    // 3 = by groupId + funcId  (criteriaValue = "groupId:funcId")
    // 4 = by isActive           (criteriaValue = "true" or "false")
    // 5 = by groupId + isActive (criteriaValue = "groupId:true/false")
    @JsonProperty("criteria_type")
    private Integer criteriaType;

    @JsonProperty("criteria_value")
    private String criteriaValue;

    private int page = 1;
    private int size = 10;
}
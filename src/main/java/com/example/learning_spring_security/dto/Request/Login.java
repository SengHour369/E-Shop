package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;


public record Login(
        Long CriteriaType,
        String CriteriaValue,
        String Password
){
}

package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;


public record Login(
        String CriteriaValue,
        String Password
){
}

package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResponseErrorTemplate(
        String message,
        String code,
        @JsonProperty("data")
        Object object){

}

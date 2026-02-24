package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Register(
        String username,
        String password,
        String email,

        @JsonProperty("full_name") String fullName){

}


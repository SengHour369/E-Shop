package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Login request DTO
 * Can login with either username or email
 */
public record Login(
        @JsonProperty("username_or_email")
        String username,
        String password
){
}

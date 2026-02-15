package com.example.learning_spring_security.dto.Request;

public record AuthenticationRequest(
        String username,
        String password
){
}

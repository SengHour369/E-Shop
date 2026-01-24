package com.example.learning_spring_security.dto;

public record AuthenticationRequest(
        String username,
        String password
){
}

package com.example.learning_spring_security.dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Register(
        @NotBlank(message = "username is required") String username,
        @NotBlank(message = "password is required") @Size(min = 6, message = "password must be at least 6 characters") String password,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
        String phone,
        @JsonProperty("full_name") String fullName){

}


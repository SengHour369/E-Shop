package com.example.eshop.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Register(
        @jakarta.validation.constraints.NotBlank String username,
        @jakarta.validation.constraints.NotBlank String password,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email String email,
        String phone,
        @JsonProperty("full_name")
        String fullName){
    public Register {
        username = username == null ? null : username.trim();
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        phone = phone == null ? null : phone.trim();
        fullName = fullName == null ? null : fullName.trim();
    }


}


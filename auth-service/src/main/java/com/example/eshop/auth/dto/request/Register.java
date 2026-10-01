package com.example.eshop.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Register(
        String username,
        String password,
        String email,
        String phone,
        @JsonProperty("full_name")
        String fullName){

}


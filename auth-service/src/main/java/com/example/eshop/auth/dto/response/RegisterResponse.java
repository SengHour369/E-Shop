package com.example.eshop.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
@Builder
public record RegisterResponse(
        Long id,
        String username,
        String password,
        String email,
        String phone,
        @JsonProperty("full_name") String fullName,
        List<String> roles,
        @JsonProperty("created") LocalDateTime created
        ) {

}

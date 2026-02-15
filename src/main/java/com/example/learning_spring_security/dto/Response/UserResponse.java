package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
@Builder
public record UserResponse(
        String username,
        String password,
        String email,
        @JsonProperty("full_name") String fullName,
        List<String> roles,
        @JsonProperty("created") LocalDateTime created
        ) {

}

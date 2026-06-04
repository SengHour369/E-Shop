package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ResponseErrorTemplate(
        String message,
        String code,
        @JsonProperty("data")
        Object object
) {
    // Constructor for easy creation
    public static ResponseErrorTemplate success(String message, Object data) {
        return ResponseErrorTemplate.builder()
                .message(message)
                .code("200")
                .object(data)
                .build();
    }

    public static ResponseErrorTemplate error(String message, String code) {
        return ResponseErrorTemplate.builder()
                .message(message)
                .code(code)
                .object(null)
                .build();
    }
}
package com.example.learning_spring_security.Bakong.service.impl.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckTransactionRequest(
        @NotBlank
        String md5
) {
}

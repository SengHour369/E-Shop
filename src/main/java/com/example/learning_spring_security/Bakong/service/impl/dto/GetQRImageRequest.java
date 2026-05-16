package com.example.learning_spring_security.Bakong.service.impl.dto;

import jakarta.validation.constraints.NotBlank;

public record GetQRImageRequest(
        @NotBlank
        String qr,
        @NotBlank
        String md5
) {
}

package com.example.eshop.payment.bakong.dto;

import jakarta.validation.constraints.NotBlank;

public record GetQRImageRequest(
        @NotBlank
        String qr,
        @NotBlank
        String md5
) {
}

package com.example.eshop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiRequest(@NotBlank @Size(max = 4000) String message) {
}

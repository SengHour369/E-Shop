package com.example.eshop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AiChatRequest(
        @NotBlank @Size(max = 4000) String message,
        UUID conversationId,
        @Pattern(regexp = "en|km") String language) {
}

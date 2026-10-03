package com.example.eshop.ai.dto;
import jakarta.validation.constraints.*;
public record AiRequest(@NotBlank @Size(max = 4000) String message) {}

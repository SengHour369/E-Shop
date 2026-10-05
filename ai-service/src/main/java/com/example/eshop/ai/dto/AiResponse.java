package com.example.eshop.ai.dto;

import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record AiResponse(
        UUID executionId,
        String requestId,
        String traceId,
        AiIntent intent,
        AiExecutionStatus status,
        String message,
        String errorCode,
        JsonNode data) {
}

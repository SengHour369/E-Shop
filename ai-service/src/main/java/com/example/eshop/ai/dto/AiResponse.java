package com.example.eshop.ai.dto;
import java.util.UUID;
import com.example.eshop.ai.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
public record AiResponse(UUID executionId, String requestId, String traceId, AiIntent intent,
    AiExecutionStatus status, String message, String errorCode, JsonNode data) {}

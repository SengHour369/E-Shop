package com.example.eshop.ai.dto;
import com.example.eshop.ai.enums.AiIntent;
import com.fasterxml.jackson.databind.JsonNode;
public record AiIntentResult(AiIntent intent, double confidence, JsonNode parameters) {}

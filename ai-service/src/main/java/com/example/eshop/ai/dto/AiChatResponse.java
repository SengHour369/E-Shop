package com.example.eshop.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiChatResponse(
        UUID conversationId,
        String mode,
        String cardType,
        List<String> suggestions,
        AiResponse result,
        ChatPresentation presentation) {
    public AiChatResponse(UUID conversationId, String mode, String cardType, List<String> suggestions, AiResponse result) {
        this(conversationId, mode, cardType, suggestions, result, null);
    }
}

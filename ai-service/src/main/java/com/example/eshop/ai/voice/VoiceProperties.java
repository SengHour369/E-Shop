package com.example.eshop.ai.voice;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("gemini.live")
public record VoiceProperties(
        boolean enabled,
        String model,
        @AssertTrue boolean freeOnly,
        @AssertFalse boolean paidFallbackEnabled,
        boolean freeProjectVerified,
        @Min(1) @Max(5) int maxCallsPerDay,
        @Min(30) @Max(300) int maxCallDurationSeconds,
        @Min(1) @Max(1) int maxConcurrentCalls
) {

    public boolean allowed() {
        return enabled
                && freeOnly
                && !paidFallbackEnabled
                && freeProjectVerified
                && "gemini-3.1-flash-live-preview".equals(model);
    }
}
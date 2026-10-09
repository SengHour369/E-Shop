package com.example.eshop.ai.service;

import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.common.security.CurrentActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiConfirmationService {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public JsonNode prepare(AiIntent intent, JsonNode parameters) {
        UUID id = UUID.randomUUID();
        try {
            String value = mapper.writeValueAsString(new Pending(intent, parameters));
            redis.opsForValue().set(key(id), value, Duration.ofMinutes(5));
            return mapper.createObjectNode()
                    .put("confirmationId", id.toString())
                    .put("tool", intent.name())
                    .put("expiresInSeconds", 300)
                    .set("parameters", parameters.deepCopy());
        } catch (Exception exception) {
            throw new AiFailure("CONFIRMATION_UNAVAILABLE", AiExecutionStatus.FAILURE,
                    "The confirmation could not be prepared. No action was taken.");
        }
    }

    public Pending consume(UUID id) {
        try {
            String value = redis.opsForValue().getAndDelete(key(id));
            if (value == null) {
                throw new IllegalArgumentException("Expired confirmation");
            }
            return mapper.readValue(value, Pending.class);
        } catch (Exception exception) {
            throw new AiFailure("CONFIRMATION_INVALID", AiExecutionStatus.DENIED,
                    "This confirmation is expired, already used, or unavailable.");
        }
    }

    private static String key(UUID id) {
        return "ai:confirmation:" + CurrentActor.userId() + ":" + id;
    }

    public record Pending(AiIntent intent, JsonNode parameters) {
    }
}

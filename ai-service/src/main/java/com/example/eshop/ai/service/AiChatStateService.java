package com.example.eshop.ai.service;

import com.example.eshop.ai.registry.AiToolRegistry;
import com.example.eshop.common.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatStateService {

    private static final DefaultRedisScript<Long> RATE_LIMIT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], 60) end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;

    public String withContext(UUID conversationId, String message) {
        if (!AiToolRegistry.authenticated()) {
            return message;
        }
        String context = redis.opsForValue().get(conversationKey(conversationId));
        if (context == null || context.equals("{}")) {
            return message;
        }
        return message + "\nPrevious backend result references (fetch current details before answering): " + context;
    }

    public void remember(UUID conversationId, com.example.eshop.ai.dto.AiResponse response) {
        if (!AiToolRegistry.authenticated()
                || response.status() != com.example.eshop.ai.enums.AiExecutionStatus.SUCCESS
                || response.data() == null) {
            return;
        }
        var context = mapper.createObjectNode();
        var data = response.data();
        if (response.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_SEARCH && data.isArray()) {
            var ids = context.putArray("productIdsInDisplayOrder");
            data.forEach(product -> {
                if (product.path("id").isIntegralNumber() && ids.size() < 20) {
                    ids.add(product.path("id").asLong());
                }
            });
        } else if (response.intent() == com.example.eshop.ai.enums.AiIntent.PRODUCT_GET) {
            context.set("selectedProductId", data.path("id"));
        } else if (response.intent() == com.example.eshop.ai.enums.AiIntent.SKU_GET) {
            context.set("selectedSkuId", data.path("id"));
        } else if (response.intent() == com.example.eshop.ai.enums.AiIntent.MY_ORDER_STATUS
                || response.intent() == com.example.eshop.ai.enums.AiIntent.ORDER_GET) {
            context.set("selectedOrderNumber", data.path("orderNumber"));
        }
        if (!context.isEmpty()) {
            redis.opsForValue().set(conversationKey(conversationId), context.toString(), Duration.ofMinutes(30));
        }
    }

    private static String conversationKey(UUID id) {
        return "ai:conversation:" + CurrentActor.userId() + ":" + id;
    }

    public void rateLimit(String remoteAddress) {
        String actor = AiToolRegistry.authenticated() ? "user:" + CurrentActor.userId() : "ip:" + remoteAddress;
        try {
            Long count = redis.execute(RATE_LIMIT, List.of("ai:rate:" + actor));
            if (count == null) {
                throw new IllegalStateException("Rate limit unavailable");
            }
            if (count > 20) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before sending another message.");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chat is temporarily unavailable.");
        }
    }

    public UUID conversation(UUID requested) {
        if (!AiToolRegistry.authenticated()) {
            return UUID.randomUUID();
        }
        UUID id = requested == null ? UUID.randomUUID() : requested;
        String key = conversationKey(id);
        try {
            if (requested != null && !Boolean.TRUE.equals(redis.hasKey(key))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found or expired.");
            }
            if (requested == null) {
                redis.opsForValue().set(key, "{}", Duration.ofMinutes(30));
            } else {
                redis.expire(key, Duration.ofMinutes(30));
            }
            return id;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Conversation storage is unavailable.");
        }
    }
}

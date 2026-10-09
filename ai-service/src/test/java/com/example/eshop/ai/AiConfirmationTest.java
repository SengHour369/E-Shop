package com.example.eshop.ai;

import com.example.eshop.ai.enums.AiIntent;
import com.example.eshop.ai.service.AiConfirmationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiConfirmationTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void confirmationIsOwnerBoundExpiringAndConsumedOnce() throws Exception {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        Map<String, String> state = new HashMap<>();
        when(redis.opsForValue()).thenReturn(values);
        doAnswer(call -> {
            assertThat(call.<Duration>getArgument(2)).isEqualTo(Duration.ofMinutes(5));
            state.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        when(values.getAndDelete(anyString())).thenAnswer(call -> state.remove(call.getArgument(0)));
        var mapper = new ObjectMapper();
        var service = new AiConfirmationService(redis, mapper);
        authenticate(7);
        var card = service.prepare(AiIntent.PROMOTION_CREATE, mapper.readTree("{\"skuId\":42}"));
        UUID id = UUID.fromString(card.path("confirmationId").asText());
        authenticate(8);
        assertThatThrownBy(() -> service.consume(id)).hasMessageContaining("expired");
        authenticate(7);
        assertThat(service.consume(id).parameters().path("skuId").asLong()).isEqualTo(42);
        assertThatThrownBy(() -> service.consume(id)).hasMessageContaining("already used");
    }

    private static void authenticate(long userId) {
        var authentication = new UsernamePasswordAuthenticationToken("user", null, List.of());
        authentication.setDetails(Map.of("userId", userId));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

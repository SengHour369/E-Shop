package com.example.eshop.common.redis;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStartupCheckTest {
    @Test
    void recordsThatTheServiceReachedRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.application.name", "eshop")).thenReturn("catalog-service");

        new RedisStartupCheck(redis, environment).run(new DefaultApplicationArguments());

        verify(values).set("catalog-service:redis", "up", Duration.ofMinutes(2));
    }

    @Test
    void keepsStartingWhenRedisIsDown() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.application.name", "eshop")).thenReturn("auth-service");

        assertThatCode(() -> new RedisStartupCheck(redis, environment).run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}

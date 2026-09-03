package com.example.learning_spring_security.integration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class RedisCacheSmokeTest {

    @Test
    public void redisPutGetSmoke() {
        DockerImageName redisImage = DockerImageName.parse("redis:6-alpine");
        try (GenericContainer<?> redis = new GenericContainer<>(redisImage).withExposedPorts(6379)) {
            redis.start();
            String host = redis.getHost();
            Integer port = redis.getMappedPort(6379);

            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(host, port);
            connectionFactory.afterPropertiesSet();

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();

            String key = "smoke:test:key";
            String value = "hello-redis";

            template.opsForValue().set(key, value);
            Object got = template.opsForValue().get(key);

            Assertions.assertEquals(value, got);

            connectionFactory.destroy();
        }
    }
}

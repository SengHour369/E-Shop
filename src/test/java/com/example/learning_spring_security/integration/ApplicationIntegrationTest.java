package com.example.learning_spring_security.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class ApplicationIntegrationTest {

    // Postgres container
    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"))
            .withDatabaseName("itestdb")
            .withUsername("itest")
            .withPassword("itest");

    // Redis container (no auth for tests)
    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // also set spring.redis.port for older config keys
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    ApplicationContext ctx;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Test
    public void contextLoads() {
        assertNotNull(ctx, "Application context should load");
    }

    @Test
    public void redisPutGetWorks() {
        String key = "it:test:key";
        String value = "hello-it";
        redisTemplate.opsForValue().set(key, value);
        Object got = redisTemplate.opsForValue().get(key);
        assertEquals(value, got);
    }
}

package com.example.learning_spring_security.Redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Custom TTL per cache region (adjust values as needed)
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("addresses", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("products", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("categories", defaultConfig.entryTtl(Duration.ofMinutes(20)))
                .withCacheConfiguration("subCategories", defaultConfig.entryTtl(Duration.ofMinutes(20)))
                .withCacheConfiguration("inventory", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("orders", defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("payments", defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("carts", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofHours(2)))
                .withCacheConfiguration("functions", defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("groups", defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("permissions", defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("cancelations", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("refunds", defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("returns", defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("categoryIcons", defaultConfig.entryTtl(Duration.ofHours(2)))
                .build();
    }
}
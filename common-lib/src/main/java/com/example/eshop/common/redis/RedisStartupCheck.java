package com.example.eshop.common.redis;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Records that this service can reach Redis. A missed ping does not stop startup. */
@Component
public class RedisStartupCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RedisStartupCheck.class);
    private final StringRedisTemplate redis;
    private final Environment environment;

    public RedisStartupCheck(StringRedisTemplate redis, Environment environment) {
        this.redis = redis;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String service = environment.getProperty("spring.application.name", "eshop");
        try {
            redis.opsForValue().set(service + ":redis", "up", Duration.ofMinutes(2));
            log.info("Redis is reachable for {}", service);
        } catch (RuntimeException ex) {
            log.warn("Redis is not reachable for {}: {}", service, ex.getClass().getSimpleName());
        }
    }
}

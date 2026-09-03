package com.example.learning_spring_security.Redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RedisConnectionConfig {

    @Value("${spring.data.redis.host:#{null}}")
    private String dataHost;
    @Value("${spring.data.redis.port:6379}")
    private int dataPort;
    @Value("${spring.data.redis.username:#{null}}")
    private String dataUser;
    @Value("${spring.data.redis.password:#{null}}")
    private String dataPassword;
    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean dataSsl;

    @Value("${spring.redis.host:#{null}}")
    private String host;
    @Value("${spring.redis.port:#{null}}")
    private Integer port;
    @Value("${spring.redis.username:#{null}}")
    private String user;
    @Value("${spring.redis.password:#{null}}")
    private String password;
    @Value("${spring.redis.ssl:false}")
    private boolean ssl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        String h = (dataHost != null) ? dataHost : host;
        int p;
        if (dataHost != null) {
            p = dataPort;
        } else if (port != null) {
            p = port;
        } else {
            p = 6379;
        }
        String u = (dataUser != null) ? dataUser : user;
        String pwd = (dataPassword != null) ? dataPassword : password;
        boolean useSsl = (dataHost != null) ? dataSsl : ssl;

        if (h == null || h.isEmpty()) {
            // fallback to localhost
            h = "localhost";
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(h, p);
        if (u != null && !u.isEmpty()) config.setUsername(u);
        if (pwd != null && !pwd.isEmpty()) config.setPassword(RedisPassword.of(pwd));

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofSeconds(1))
                .useSsl(useSsl)
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }
}

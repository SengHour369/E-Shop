package com.example.eshop.catalog.config;

import com.example.eshop.catalog.scanner.HttpProductVisionClient;
import com.example.eshop.catalog.scanner.ProductVisionClient;
import com.example.eshop.catalog.scanner.ScanCoalescer;
import com.example.eshop.catalog.scanner.ScanRateLimiter;
import com.example.eshop.catalog.scanner.ScannerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ScannerProperties.class)
public class ScannerConfiguration {
    @Bean
    ScanCoalescer scanCoalescer(ScannerProperties properties) {
        return new ScanCoalescer(properties.getDuplicateWindowMs(), System::currentTimeMillis);
    }

    @Bean
    ScanRateLimiter scanRateLimiter(ScannerProperties properties) {
        return new ScanRateLimiter(properties.getRateLimit(), properties.getRateWindowSeconds(), System::currentTimeMillis);
    }

    @Bean
    ProductVisionClient productVisionClient(ScannerProperties properties, ObjectMapper mapper) {
        return new HttpProductVisionClient(mapper, properties.getAiInferenceUrl(),
                Duration.ofSeconds(Math.max(1, properties.getAiTimeoutSeconds())));
    }
}

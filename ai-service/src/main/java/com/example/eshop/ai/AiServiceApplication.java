package com.example.eshop.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.example.eshop.ai", "com.example.eshop.common"})
@EntityScan({"com.example.eshop.ai.model", "com.example.eshop.common.audit"})
@EnableJpaRepositories({"com.example.eshop.ai.repository", "com.example.eshop.common.audit"})
@EnableFeignClients
@EnableScheduling
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}

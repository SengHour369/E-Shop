package com.example.eshop.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@EnableFeignClients
@org.springframework.boot.autoconfigure.domain.EntityScan({"com.example.eshop.auth.model", "com.example.eshop.common.audit"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories({"com.example.eshop.auth.repository", "com.example.eshop.common.audit"})
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.eshop.auth", "com.example.eshop.common"})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

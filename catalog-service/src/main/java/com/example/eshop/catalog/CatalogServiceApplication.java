package com.example.eshop.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@EnableFeignClients
@org.springframework.boot.autoconfigure.domain.EntityScan({"com.example.eshop.catalog.model", "com.example.eshop.common.audit"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories({"com.example.eshop.catalog.repository", "com.example.eshop.common.audit"})
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.eshop.catalog", "com.example.eshop.common"})
public class CatalogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}

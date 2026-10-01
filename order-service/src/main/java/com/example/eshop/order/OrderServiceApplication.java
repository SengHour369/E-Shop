package com.example.eshop.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@EnableFeignClients
@org.springframework.boot.autoconfigure.domain.EntityScan({"com.example.eshop.order.model", "com.example.eshop.common.audit"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories({"com.example.eshop.order.repository", "com.example.eshop.common.audit"})
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.eshop.order", "com.example.eshop.common"})
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

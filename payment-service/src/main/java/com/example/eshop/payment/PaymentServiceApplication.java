package com.example.eshop.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@EnableFeignClients
@org.springframework.boot.autoconfigure.domain.EntityScan({"com.example.eshop.payment.model", "com.example.eshop.common.audit"})
@org.springframework.data.jpa.repository.config.EnableJpaRepositories({"com.example.eshop.payment.repository", "com.example.eshop.common.audit"})
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.eshop.payment", "com.example.eshop.common"})
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

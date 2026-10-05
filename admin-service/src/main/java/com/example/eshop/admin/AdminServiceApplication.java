package com.example.eshop.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.example.eshop.admin", "com.example.eshop.common"})
@EntityScan({"com.example.eshop.admin.model", "com.example.eshop.common.audit"})
@EnableJpaRepositories({"com.example.eshop.admin.repository", "com.example.eshop.common.audit"})
@EnableFeignClients
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}

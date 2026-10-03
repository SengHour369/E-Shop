package com.example.eshop.notification;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication(scanBasePackages={"com.example.eshop.notification","com.example.eshop.common"})
@EntityScan({"com.example.eshop.notification.model","com.example.eshop.common.audit"})
@EnableJpaRepositories({"com.example.eshop.notification.repository","com.example.eshop.common.audit"})
@EnableFeignClients @EnableScheduling
public class NotificationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(NotificationServiceApplication.class,args); }
}

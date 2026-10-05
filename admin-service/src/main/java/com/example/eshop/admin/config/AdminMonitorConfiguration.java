package com.example.eshop.admin.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminMonitorConfiguration {

    @Bean
    Clock adminClock() {
        return Clock.systemUTC();
    }
}

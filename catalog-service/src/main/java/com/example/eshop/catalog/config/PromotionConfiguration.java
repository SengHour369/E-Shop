package com.example.eshop.catalog.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import java.time.*;
@Configuration @EnableScheduling
public class PromotionConfiguration {
    @Bean public Clock promotionClock(@Value("${app.time-zone:UTC}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}


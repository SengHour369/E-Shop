package com.example.eshop.admin.health;

import com.example.eshop.admin.service.AdminMonitorService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class AdminMonitorHealthIndicator implements HealthIndicator {

    private final AdminMonitorService monitor;

    public AdminMonitorHealthIndicator(AdminMonitorService monitor) {
        this.monitor = monitor;
    }

    @Override
    public Health health() {
        return monitor.healthy() ? Health.up().build() : Health.down().build();
    }
}

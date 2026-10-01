package com.example.eshop.gateway.repository.logging;

import com.example.eshop.gateway.logging.GatewayRequestLog;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface GatewayRequestLogRepository
    extends ReactiveCrudRepository<GatewayRequestLog, Long> {}

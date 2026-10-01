package com.example.eshop.gateway.controller;

import com.example.eshop.gateway.service.GatewayFallbackService;
import com.example.eshop.gateway.service.GatewayFallbackService.FallbackResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Reached when a route's circuit breaker is open (the target service is down or timing out) instead
 * of the caller getting a raw connection-refused error.
 */
@RestController

public class FallbackController {

  private final GatewayFallbackService fallbackService;

  public FallbackController(GatewayFallbackService fallbackService) {
    this.fallbackService = fallbackService;
  }

  @RequestMapping("/fallback/{service}")
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public Mono<FallbackResponse> fallback(@PathVariable String service) {
    return fallbackService.unavailable(service);
  }
}

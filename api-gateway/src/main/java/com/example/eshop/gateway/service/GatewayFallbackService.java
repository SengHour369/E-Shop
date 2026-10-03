package com.example.eshop.gateway.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Builds the stable error contract returned when an upstream service is unavailable. */
@Service
public class GatewayFallbackService {

  private final Clock clock;

  public GatewayFallbackService() {
    this(Clock.systemUTC());
  }

  GatewayFallbackService(Clock clock) {
    this.clock = clock;
  }

  public Mono<FallbackResponse> unavailable(String service) {
    return Mono.deferContextual(context -> Mono.just(
        new FallbackResponse(
            Instant.now(clock),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            service,
            "ai".equals(service) ? "AI response unavailable. Check execution history and reuse the same Idempotency-Key if retrying." : service + " is temporarily unavailable — please retry shortly.", context.getOrDefault("requestId", "unknown"))));
  }

  public record FallbackResponse(Instant timestamp, int status, String service, String message, String requestId) {}
}

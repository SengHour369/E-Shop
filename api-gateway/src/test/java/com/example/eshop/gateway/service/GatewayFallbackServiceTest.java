package com.example.eshop.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class GatewayFallbackServiceTest {

  @Test
  void createsStableServiceUnavailableResponse() {
    var now = Instant.parse("2026-09-19T08:00:00Z");
    var service = new GatewayFallbackService(Clock.fixed(now, ZoneOffset.UTC));

    var response = service.unavailable("orders").block();

    assertThat(response).isNotNull();
    assertThat(response.timestamp()).isEqualTo(now);
    assertThat(response.status()).isEqualTo(503);
    assertThat(response.service()).isEqualTo("orders");
    assertThat(response.message()).contains("temporarily unavailable");
  }
}

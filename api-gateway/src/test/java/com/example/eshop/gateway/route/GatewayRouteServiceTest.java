package com.example.eshop.gateway.route;

import com.example.eshop.gateway.repository.route.GatewayRouteRepository;
import com.example.eshop.gateway.service.route.GatewayRouteService;
import com.example.eshop.gateway.dto.route.GatewayRouteRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class GatewayRouteServiceTest {

  @Test
  void duplicateRouteKeyReturnsConflictWithoutSaving() {
    GatewayRouteRepository repository = mock(GatewayRouteRepository.class);
    ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    GatewayRouteService service = new GatewayRouteService(repository, events);
    GatewayRoute existing = new GatewayRoute();
    existing.setId(7L);
    existing.setRouteKey("analytics-service");
    when(repository.findByRouteKey("analytics-service")).thenReturn(Mono.just(existing));

    ResponseStatusException response =
        assertThrows(ResponseStatusException.class, () -> service.create(request()).block());

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("Gateway route already exists: analytics-service", response.getReason());

    verify(repository, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  private GatewayRouteRequest request() {
    return new GatewayRouteRequest(
        "analytics-service",
        "lb://analytics-service",
        "/api/analytics/**",
        "GET",
        true,
        120,
        60);
  }
}

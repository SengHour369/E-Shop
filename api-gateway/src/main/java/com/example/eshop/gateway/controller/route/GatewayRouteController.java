package com.example.eshop.gateway.controller.route;

import com.example.eshop.gateway.route.GatewayRoute;
import com.example.eshop.gateway.dto.route.GatewayRouteRequest;
import com.example.eshop.gateway.service.route.GatewayRouteService;

import com.example.eshop.gateway.service.security.GatewayAdminKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/gateway/routes")
public class GatewayRouteController {
  private final GatewayRouteService service;
  private final GatewayAdminKeyService security;

  public GatewayRouteController(GatewayRouteService service, GatewayAdminKeyService security) {
    this.service = service;
    this.security = security;
  }

  @GetMapping
  public Flux<GatewayRoute> findAll(@RequestHeader(value = "X-Gateway-Admin-Key", required = false) String adminKey) {
    security.requireAdmin(adminKey);
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<GatewayRoute> create(
      @RequestHeader(value = "X-Gateway-Admin-Key", required = false) String adminKey,
      @Valid @RequestBody GatewayRouteRequest request) {
    security.requireAdmin(adminKey);
    return service.create(request);
  }

  @PutMapping("/{id}")
  public Mono<GatewayRoute> update(
      @RequestHeader(value = "X-Gateway-Admin-Key", required = false) String adminKey,
      @PathVariable Long id,
      @Valid @RequestBody GatewayRouteRequest request) {
    security.requireAdmin(adminKey);
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(
      @RequestHeader(value = "X-Gateway-Admin-Key", required = false) String adminKey, @PathVariable Long id) {
    security.requireAdmin(adminKey);
    return service.delete(id);
  }

  @PostMapping("/refresh")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refresh(@RequestHeader(value = "X-Gateway-Admin-Key", required = false) String adminKey) {
    security.requireAdmin(adminKey);
    service.refresh();
  }

}

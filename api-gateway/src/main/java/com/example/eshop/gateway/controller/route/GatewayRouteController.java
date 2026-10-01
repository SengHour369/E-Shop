package com.example.eshop.gateway.controller.route;

import com.example.eshop.gateway.route.GatewayRoute;
import com.example.eshop.gateway.dto.route.GatewayRouteRequest;
import com.example.eshop.gateway.service.route.GatewayRouteService;

import com.example.eshop.gateway.config.GatewaySecurityProperties;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/gateway/routes")
public class GatewayRouteController {
  private final GatewayRouteService service;
  private final GatewaySecurityProperties security;

  public GatewayRouteController(GatewayRouteService service, GatewaySecurityProperties security) {
    this.service = service;
    this.security = security;
  }

  @GetMapping
  public Flux<GatewayRoute> findAll(@RequestHeader("X-Gateway-Admin-Key") String adminKey) {
    requireAdmin(adminKey);
    return service.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<GatewayRoute> create(
      @RequestHeader("X-Gateway-Admin-Key") String adminKey,
      @Valid @RequestBody GatewayRouteRequest request) {
    requireAdmin(adminKey);
    return service.create(request);
  }

  @PutMapping("/{id}")
  public Mono<GatewayRoute> update(
      @RequestHeader("X-Gateway-Admin-Key") String adminKey,
      @PathVariable Long id,
      @Valid @RequestBody GatewayRouteRequest request) {
    requireAdmin(adminKey);
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(
      @RequestHeader("X-Gateway-Admin-Key") String adminKey, @PathVariable Long id) {
    requireAdmin(adminKey);
    return service.delete(id);
  }

  @PostMapping("/refresh")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void refresh(@RequestHeader("X-Gateway-Admin-Key") String adminKey) {
    requireAdmin(adminKey);
    service.refresh();
  }

  private void requireAdmin(String suppliedKey) {
    String expectedKey = security.getAdminKey();
    boolean matches =
        expectedKey != null && !expectedKey.isBlank()
            && suppliedKey != null
            && MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8));
    if (!matches) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid gateway admin key");
    }
  }
}

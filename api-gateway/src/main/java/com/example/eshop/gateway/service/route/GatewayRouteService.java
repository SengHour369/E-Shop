package com.example.eshop.gateway.service.route;

import com.example.eshop.gateway.route.GatewayRoute;
import com.example.eshop.gateway.repository.route.GatewayRouteRepository;
import com.example.eshop.gateway.dto.route.GatewayRouteRequest;

import java.time.Instant;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class GatewayRouteService {
  private final GatewayRouteRepository repository;
  private final ApplicationEventPublisher events;

  public GatewayRouteService(GatewayRouteRepository repository, ApplicationEventPublisher events) {
    this.repository = repository;
    this.events = events;
  }

  public Flux<GatewayRoute> findAll() {
    return repository.findAll();
  }

  public Mono<GatewayRoute> create(GatewayRouteRequest request) {
    GatewayRoute route = apply(new GatewayRoute(), request);
    route.setCreatedAt(Instant.now());
    return repository
        .findByRouteKey(request.routeKey())
        .flatMap(existing -> Mono.<GatewayRoute>error(duplicateRoute(request.routeKey())))
        .switchIfEmpty(Mono.defer(() -> repository.save(route)))
        .onErrorMap(
            DuplicateKeyException.class, ignored -> duplicateRoute(request.routeKey()))
        .doOnSuccess(saved -> refresh());
  }

  public Mono<GatewayRoute> update(Long id, GatewayRouteRequest request) {
    return repository
        .findById(id)
        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Gateway route not found: " + id)))
        .map(route -> apply(route, request))
        .flatMap(repository::save)
        .onErrorMap(DuplicateKeyException.class, ignored -> duplicateRoute(request.routeKey()))
        .doOnSuccess(saved -> refresh());
  }

  public Mono<Void> delete(Long id) {
    return repository
        .existsById(id)
        .flatMap(
            exists ->
                exists
                    ? repository.deleteById(id)
                    : Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Gateway route not found: " + id)))
        .doOnSuccess(ignored -> refresh());
  }

  public void refresh() {
    events.publishEvent(new RefreshRoutesEvent(this));
  }

  private GatewayRoute apply(GatewayRoute route, GatewayRouteRequest request) {
    try {
      new org.springframework.web.util.pattern.PathPatternParser().parse(request.pathPattern());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid route path pattern");
    }
    if ((request.rateLimit() == null) != (request.rateLimitWindowSeconds() == null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Set both rate limit and window, or neither");
    }
    route.setRouteKey(request.routeKey());
    route.setUri(request.uri());
    route.setPathPattern(request.pathPattern());
    route.setHttpMethod(request.httpMethod());
    route.setEnabled(request.enabled());
    route.setRateLimit(request.rateLimit());
    route.setRateLimitWindowSeconds(request.rateLimitWindowSeconds());
    route.setUpdatedAt(Instant.now());
    return route;
  }

  private ResponseStatusException duplicateRoute(String routeKey) {
    return new ResponseStatusException(
        HttpStatus.CONFLICT, "Gateway route already exists: " + routeKey);
  }
}

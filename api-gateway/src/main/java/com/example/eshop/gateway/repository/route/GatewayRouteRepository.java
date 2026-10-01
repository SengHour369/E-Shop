package com.example.eshop.gateway.repository.route;

import com.example.eshop.gateway.route.GatewayRoute;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GatewayRouteRepository extends ReactiveCrudRepository<GatewayRoute, Long> {
  Flux<GatewayRoute> findByEnabledTrue();

  Mono<GatewayRoute> findByRouteKey(String routeKey);
}

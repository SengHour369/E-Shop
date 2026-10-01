package com.example.eshop.gateway.route;

import com.example.eshop.gateway.repository.route.GatewayRouteRepository;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnProperty(name = "gateway.dynamic-routes-enabled", havingValue = "true")
public class DynamicRouteDefinitionLocator implements RouteDefinitionLocator {
  private final GatewayRouteRepository repository;

  public DynamicRouteDefinitionLocator(GatewayRouteRepository repository) {
    this.repository = repository;
  }

  @Override
  public Flux<RouteDefinition> getRouteDefinitions() {
    return repository.findByEnabledTrue().map(this::definition);
  }

  private RouteDefinition definition(GatewayRoute route) {
    RouteDefinition definition = new RouteDefinition();
    definition.setId("dynamic-" + route.getRouteKey());
    definition.setUri(URI.create(route.getUri()));

    PredicateDefinition path = new PredicateDefinition();
    path.setName("Path");
    path.addArg(NameUtils.GENERATED_NAME_PREFIX + "0", route.getPathPattern());
    definition.getPredicates().add(path);

    if (StringUtils.hasText(route.getHttpMethod())) {
      PredicateDefinition method = new PredicateDefinition();
      method.setName("Method");
      method.addArg(NameUtils.GENERATED_NAME_PREFIX + "0", route.getHttpMethod());
      definition.getPredicates().add(method);
    }

    FilterDefinition circuitBreaker = new FilterDefinition();
    circuitBreaker.setName("CircuitBreaker");
    circuitBreaker.addArg("name", route.getRouteKey() + "CircuitBreaker");
    circuitBreaker.addArg("fallbackUri", "forward:/fallback/" + route.getRouteKey());
    definition.getFilters().add(circuitBreaker);
    return definition;
  }
}

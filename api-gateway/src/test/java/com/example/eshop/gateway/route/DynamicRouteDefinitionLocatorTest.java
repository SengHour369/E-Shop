package com.example.eshop.gateway.route;

import com.example.eshop.gateway.repository.route.GatewayRouteRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DynamicRouteDefinitionLocatorTest {
  @Test void createsMethodConstrainedEurekaRouteWithFallback() {
    var repository = mock(GatewayRouteRepository.class);
    var route = new GatewayRoute();
    route.setRouteKey("recommendations");
    route.setUri("lb://recommendation-service");
    route.setPathPattern("/api/v1/recommendations/**");
    route.setHttpMethod("GET");
    when(repository.findByEnabledTrue()).thenReturn(Flux.just(route));
    var result = new DynamicRouteDefinitionLocator(repository).getRouteDefinitions().blockFirst();
    assertThat(result.getId()).isEqualTo("dynamic-recommendations");
    assertThat(result.getUri().toString()).isEqualTo("lb://recommendation-service");
    assertThat(result.getPredicates()).extracting(predicate -> predicate.getName()).containsExactly("Path", "Method");
    assertThat(result.getFilters().get(0).getArgs()).containsEntry("fallbackUri", "forward:/fallback/recommendations");
  }
}

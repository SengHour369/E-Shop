package com.example.eshop.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
    "gateway.dynamic-routes-enabled=false", "gateway.logging.database-enabled=false",
    "spring.sql.init.mode=never", "gateway.security.admin-key=test-admin"
})
class GatewayRouteConfigurationTest {
  @org.springframework.test.context.bean.override.mockito.MockitoBean
  com.example.eshop.gateway.service.security.GatewayAdminKeyService adminKeys;
  @Autowired RouteDefinitionLocator definitions;
  @Autowired WebTestClient client;
  @Autowired org.springframework.cloud.gateway.config.GlobalCorsProperties cors;

  @Test void routesMatchEshopControllersAndPreservePaths() {
    var routes = definitions.getRouteDefinitions().collectList().block().stream()
        .collect(Collectors.toMap(route -> route.getId(), Function.identity()));
    assertThat(routes).containsKeys("auth-service", "catalog-service", "order-service", "payment-service");
    assertThat(routes.get("auth-service").getPredicates().get(0).getArgs().values())
        .anyMatch(value -> value.contains("/api/v1/public/**"))
        .anyMatch(value -> value.contains("/api/v1/user-permissions/**"));
    assertThat(routes.get("catalog-service").getPredicates().get(0).getArgs().values())
        .anyMatch(value -> value.contains("/api/v1/category-icons/**"));
    assertThat(routes.values().stream().filter(route -> !route.getId().endsWith("-openapi")))
        .allSatisfy(route -> {
          assertThat(route.getFilters()).anyMatch(filter -> filter.getName().equals("CircuitBreaker"));
          assertThat(route.getFilters()).noneMatch(filter -> filter.getName().equals("StripPrefix"));
        });
  }

  @Test void missingTokenIsRejectedBeforeUnavailableService() {
    client.get().uri("/api/v1/orders").exchange().expectStatus().isUnauthorized()
        .expectHeader().exists("X-Correlation-Id").expectHeader().exists("X-Request-ID");
  }

  @Test void publicLoginReturnsStableFallbackWhenServiceIsUnavailable() {
    client.post().uri("/api/v1/public/email/username/login").exchange()
        .expectStatus().isEqualTo(503).expectHeader().exists("X-Correlation-Id")
        .expectBody().jsonPath("$.service").isEqualTo("auth").jsonPath("$.status").isEqualTo(503);
  }

  @Test void dynamicRouteAdminRequiresCorrectKey() {
    org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.FORBIDDEN)).when(adminKeys).requireAdmin("wrong");
    client.get().uri("/api/v1/gateway/routes").header("X-Gateway-Admin-Key", "wrong")
        .exchange().expectStatus().isForbidden();
  }

  @Test void preflightDoesNotNeedBearerToken() {
    assertThat(cors.getCorsConfigurations().get("/**").getAllowedOrigins()).contains("http://localhost:3000");
    client.options().uri("http://localhost:8080/api/v1/orders").header("Origin", "http://localhost:3000")
        .header("Access-Control-Request-Method", "POST").exchange().expectStatus().isOk()
        .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
  }

  @Test void localNotFoundAlsoHasRequestId() {
    client.get().uri("/missing").header("X-Request-ID", "req_missing").exchange()
        .expectStatus().isNotFound().expectHeader().valueEquals("X-Request-ID", "req_missing")
        .expectBody().jsonPath("$.requestId").isEqualTo("req_missing");
  }
}

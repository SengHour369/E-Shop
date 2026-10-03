package com.example.eshop.gateway.service.security;

import com.example.eshop.gateway.config.GatewaySecurityProperties;
import com.example.eshop.gateway.repository.security.GatewayAdminKeyRepository;
import com.example.eshop.gateway.controller.route.GatewayRouteController;
import com.example.eshop.gateway.service.route.GatewayRouteService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayAdminKeyServiceTest {
  private final GatewayAdminKeyRepository repository = mock(GatewayAdminKeyRepository.class);
  private final AtomicReference<String> stored = new AtomicReference<>();

  private GatewayAdminKeyService service(String configured) {
    var properties = new GatewaySecurityProperties();
    properties.setAdminKey(configured);
    return new GatewayAdminKeyService(repository, properties);
  }

  private void database() {
    when(repository.insertIfAbsent(anyString())).thenAnswer(invocation ->
        Mono.fromRunnable(() -> stored.compareAndSet(null, invocation.getArgument(0))));
    when(repository.findKey()).thenAnswer(invocation -> Mono.defer(() -> Mono.justOrEmpty(stored.get())));
  }

  @Test void generatesPersistsAndReusesKeyAcrossRestarts() {
    database();
    var first = service(null); first.initialize().block();
    String key = stored.get();
    assertThat(key).matches("gak_[A-Za-z0-9_-]{43}");
    first.requireAdmin(key);
    var restarted = service("different-environment-key"); restarted.initialize().block();
    assertThat(stored.get()).isEqualTo(key);
    restarted.requireAdmin(key);
    assertThatThrownBy(() -> restarted.requireAdmin("different-environment-key"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test void configuredKeySeedsEmptyDatabase() {
    database();
    var keys = service("existing-configured-secret"); keys.initialize().block();
    assertThat(stored.get()).isEqualTo("existing-configured-secret");
    keys.requireAdmin("existing-configured-secret");
  }

  @Test void independentInstancesUseTheWinningDatabaseKey() {
    database();
    var one = service(null);
    var two = service(null);
    Mono.when(one.initialize().subscribeOn(reactor.core.scheduler.Schedulers.parallel()),
        two.initialize().subscribeOn(reactor.core.scheduler.Schedulers.parallel())).block();
    one.requireAdmin(stored.get()); two.requireAdmin(stored.get());
  }

  @Test void missingDatabaseFailsInitializationAndKeepsEndpointClosed() {
    when(repository.insertIfAbsent(anyString())).thenReturn(Mono.error(new IllegalStateException("offline")));
    when(repository.findKey()).thenReturn(Mono.empty());
    var keys = service(null);
    assertThatThrownBy(() -> keys.initialize().block()).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> keys.requireAdmin("anything")).isInstanceOfSatisfying(
        ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
  }

  @Test void blankStoredKeyFailsStartup() {
    when(repository.insertIfAbsent(anyString())).thenReturn(Mono.empty());
    when(repository.findKey()).thenReturn(Mono.just(" "));
    assertThatThrownBy(() -> service(null).initialize().block()).isInstanceOf(IllegalStateException.class);
  }

  @Test void endpointRejectsMissingOrWrongKeyAndAcceptsStoredKey() {
    database();
    var keys = service(null); keys.initialize().block();
    var routes = mock(GatewayRouteService.class);
    when(routes.findAll()).thenReturn(Flux.empty());
    var client = WebTestClient.bindToController(new GatewayRouteController(routes, keys)).build();
    client.get().uri("/api/v1/gateway/routes").exchange().expectStatus().isForbidden();
    client.get().uri("/api/v1/gateway/routes").header("X-Gateway-Admin-Key", "wrong")
        .exchange().expectStatus().isForbidden();
    verifyNoInteractions(routes);
    client.get().uri("/api/v1/gateway/routes").header("X-Gateway-Admin-Key", stored.get())
        .exchange().expectStatus().isOk().expectBody().json("[]");
    verify(routes).findAll();
  }
}

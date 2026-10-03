package com.example.eshop.gateway.service.security;

import com.example.eshop.gateway.config.GatewaySecurityProperties;
import com.example.eshop.gateway.repository.security.GatewayAdminKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@DependsOnDatabaseInitialization
public class GatewayAdminKeyService implements ApplicationRunner {
  private final GatewayAdminKeyRepository repository;
  private final GatewaySecurityProperties properties;
  private volatile byte[] expectedKey;

  public GatewayAdminKeyService(GatewayAdminKeyRepository repository, GatewaySecurityProperties properties) {
    this.repository = repository;
    this.properties = properties;
  }

  @Override public void run(ApplicationArguments arguments) {
    // Blocking is restricted to startup, never a Netty request/event-loop thread.
    initialize().block(Duration.ofSeconds(30));
  }

  public Mono<Void> initialize() {
    return Mono.defer(() -> {
      String configured = properties.getAdminKey();
      String candidate = configured == null || configured.isBlank() ? generateKey() : configured;
      return repository.insertIfAbsent(candidate)
          .then(repository.findKey())
          .filter(key -> !key.isBlank())
          .switchIfEmpty(Mono.error(new IllegalStateException("Gateway admin key is missing")))
          .doOnNext(key -> expectedKey = key.getBytes(StandardCharsets.UTF_8))
          .then();
    });
  }

  public void requireAdmin(String suppliedKey) {
    byte[] expected = expectedKey;
    if (expected == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gateway admin key is not initialized");
    }
    if (suppliedKey == null || suppliedKey.length() > 512
        || !MessageDigest.isEqual(expected, suppliedKey.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid gateway admin key");
    }
  }

  private static String generateKey() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return "gak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}

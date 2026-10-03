package com.example.eshop.gateway.service.security;

import com.example.eshop.gateway.config.GatewaySecurityProperties;
import com.example.eshop.gateway.repository.security.GatewayAdminKeyRepository;
import io.r2dbc.spi.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "GATEWAY_KEY_TEST_URL", matches = ".+")
class GatewayAdminKeyPostgresTest {
  @Test void schemaAndConcurrentBootstrapPersistAcrossServiceInstances() {
    String url = System.getenv("GATEWAY_KEY_TEST_URL");
    if (!url.matches("r2dbc:postgresql://localhost:[0-9]+/gateway_key_verification_[a-z0-9]+"))
      throw new IllegalArgumentException("An isolated gateway_key_verification database is required");
    var options = ConnectionFactoryOptions.parse(url).mutate()
        .option(ConnectionFactoryOptions.USER, System.getenv("GATEWAY_KEY_TEST_USER"))
        .option(ConnectionFactoryOptions.PASSWORD, System.getenv("GATEWAY_KEY_TEST_PASSWORD")).build();
    var connections = ConnectionFactories.get(options);
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(connections).block(Duration.ofSeconds(15));
    var db = DatabaseClient.create(connections);
    var repository = new GatewayAdminKeyRepository(db);
    var first = new GatewayAdminKeyService(repository, new GatewaySecurityProperties());
    var second = new GatewayAdminKeyService(repository, new GatewaySecurityProperties());
    Mono.when(first.initialize(), second.initialize()).block(Duration.ofSeconds(15));
    String persisted = repository.findKey().block(Duration.ofSeconds(5));
    assertThat(persisted).matches("gak_[A-Za-z0-9_-]{43}");
    first.requireAdmin(persisted); second.requireAdmin(persisted);
    var config = new GatewaySecurityProperties(); config.setAdminKey("must-not-overwrite");
    var restarted = new GatewayAdminKeyService(repository, config);
    restarted.run(null);
    restarted.requireAdmin(persisted);
    assertThat(repository.findKey().block(Duration.ofSeconds(5))).isEqualTo(persisted);
    assertThat(db.sql("SELECT count(*) AS total FROM gateway_admin_keys")
        .map((row, metadata) -> row.get("total", Long.class)).one().block(Duration.ofSeconds(5))).isEqualTo(1L);
  }
}

package com.example.eshop.gateway.repository.security;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** Singleton credential: concurrent gateway starts must never overwrite an existing key. */
@Repository
public class GatewayAdminKeyRepository {
  private final DatabaseClient database;
  public GatewayAdminKeyRepository(DatabaseClient database) { this.database = database; }

  public Mono<Void> insertIfAbsent(String key) {
    return database.sql("""
        INSERT INTO gateway_admin_keys (id, admin_key) VALUES (1, :key)
        ON CONFLICT (id) DO NOTHING
        """).bind("key", key).fetch().rowsUpdated().then();
  }

  public Mono<String> findKey() {
    return findAdminKey().map(com.example.eshop.gateway.security.GatewayAdminKey::getAdminKey);
  }
  public Mono<com.example.eshop.gateway.security.GatewayAdminKey> findAdminKey() {
    return database.sql("SELECT id, admin_key, created_at FROM gateway_admin_keys WHERE id = 1")
        .map((row, metadata) -> {
          var key = new com.example.eshop.gateway.security.GatewayAdminKey();
          key.setId(row.get("id", Short.class));
          key.setAdminKey(row.get("admin_key", String.class));
          key.setCreatedAt(row.get("created_at", java.time.OffsetDateTime.class).toInstant());
          return key;
        }).one();
  }}

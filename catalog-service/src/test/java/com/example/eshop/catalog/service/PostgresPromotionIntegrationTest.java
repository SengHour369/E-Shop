package com.example.eshop.catalog.service;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Only run against a disposable database: Hibernate creates/drops its test schema. */
@EnabledIfEnvironmentVariable(named = "ESHOP_TEST_POSTGRES_URL", matches = ".+")
class PostgresPromotionIntegrationTest extends PromotionIntegrationTest {
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry properties) {
        String url = System.getenv("ESHOP_TEST_POSTGRES_URL");
        if (url == null || !url.endsWith("/promotion_verification"))
            throw new IllegalArgumentException("A disposable promotion_verification database is required");
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> System.getenv("ESHOP_TEST_POSTGRES_PASSWORD"));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
}

package com.example.eshop.catalog.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Opt-in runtime seeder. Never resets existing catalog records or user data. */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "catalog.seed.enabled", havingValue = "true")
public class CatalogDataSeeder implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Serialize startup seeders across replicas; released automatically on commit/rollback.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(731045211)");
        int categories = 0, icons = 0, brands = 0;
        for (var seed : CatalogSeedData.categories()) {
            String url = "/api/v1/category-icons/seed/" + seed.slug() + ".svg";
            categories += jdbc.update("""
                INSERT INTO categories (name, description, icon, status, deleted, created_at)
                SELECT ?, ?, ?, true, false, CURRENT_TIMESTAMP
                WHERE NOT EXISTS (SELECT 1 FROM categories WHERE lower(name) = lower(?))
                """, seed.name(), seed.group() + " products", url, seed.name());
            icons += jdbc.update("""
                INSERT INTO category_icons (name, url, created_at)
                SELECT ?, ?, CURRENT_TIMESTAMP
                WHERE NOT EXISTS (SELECT 1 FROM category_icons WHERE lower(name) = lower(?))
                """, seed.name(), url, seed.name());
        }
        for (var seed : CatalogSeedData.brands()) {
            brands += jdbc.update("""
                INSERT INTO brand_logos (name, url, created_at)
                SELECT ?, ?, CURRENT_TIMESTAMP
                WHERE NOT EXISTS (SELECT 1 FROM brand_logos WHERE lower(name) = lower(?))
                """, seed.name(), "/api/v1/brand-logos/seed/" + seed.slug() + ".svg", seed.name());
        }
        log.info("Catalog seed completed: {} categories, {} icons, {} brands added", categories, icons, brands);
    }
}

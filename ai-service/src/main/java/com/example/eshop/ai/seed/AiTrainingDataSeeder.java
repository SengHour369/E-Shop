package com.example.eshop.ai.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/** Opt-in startup seeder. Requires the existing manual ai_training migration. */
@Component
@ConditionalOnProperty(name = "ai.training.seed.enabled", havingValue = "true")
public class AiTrainingDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AiTrainingDataSeeder.class);
    private final JdbcTemplate jdbc;
    public AiTrainingDataSeeder(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        long count = seed(AiTrainingSeedData.DATASET_CODE);
        log.info("AI bootstrap dataset ready: {} synthetic draft/reviewed examples; no model training started", count);
    }

    // The caller must hold a transaction so the advisory lock covers all batches.
    long seed(String code) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT to_regclass('ai_training.examples') IS NOT NULL", Boolean.class))) {
            throw new IllegalStateException("Apply ai-service db/upgrade/20261011_ai_training.sql before enabling AI training seed");
        }
        jdbc.queryForList("SELECT pg_advisory_xact_lock(731045212)");
        jdbc.update("""
            INSERT INTO ai_training.datasets(code, name, description, created_by)
            VALUES (?, 'E-Shop synthetic bootstrap: 100,000 examples',
                '40 topics x 125 product descriptions x 10 phrasing styles x 2 languages. Human review required. Template-generated starter corpus, not a quality benchmark.',
                'ai-training-seeder-v1')
            ON CONFLICT (code) DO NOTHING
            """, code);
        // Keep the original dataset identifier so expansion preserves existing rows and reviews.
        jdbc.update("""
            UPDATE ai_training.datasets SET name = 'E-Shop synthetic bootstrap: 100,000 examples',
                description = '40 topics x 125 product descriptions x 10 phrasing styles x 2 languages. Human review required. Template-generated starter corpus, not a quality benchmark.'
            WHERE code = ? AND created_by = 'ai-training-seeder-v1'
                AND name IN ('E-Shop synthetic bootstrap: 10,000 examples', 'E-Shop synthetic bootstrap: 20,000 examples')
            """, code);
        UUID dataset = jdbc.queryForObject("SELECT id FROM ai_training.datasets WHERE code = ?", UUID.class, code);
        jdbc.batchUpdate("""
            INSERT INTO ai_training.examples(dataset_id, language, prompt, response, split, source, created_by)
            VALUES (?, ?, ?, ?, ?, 'synthetic:eshop-bootstrap-10000-v1', 'ai-training-seeder-v1')
            ON CONFLICT (dataset_id, language, content_hash) DO NOTHING
            """, AiTrainingSeedData.examples(), 250, (statement, example) -> {
                statement.setObject(1, dataset);
                statement.setString(2, example.language());
                statement.setString(3, example.prompt());
                statement.setString(4, example.response());
                statement.setString(5, example.split());
            });
        return jdbc.queryForObject("SELECT count(*) FROM ai_training.examples WHERE dataset_id = ?", Long.class, dataset);
    }

    /** Local one-shot alternative to restarting the whole microservice stack. */
    public static void main(String[] arguments) {
        String password = System.getenv("AI_TRAINING_JDBC_PASSWORD");
        if (password == null || password.isBlank()) throw new IllegalStateException("Set AI_TRAINING_JDBC_PASSWORD for the local database");
        var source = new DriverManagerDataSource(
            System.getenv().getOrDefault("AI_TRAINING_JDBC_URL", "jdbc:postgresql://localhost:5437/ai_db"),
            System.getenv().getOrDefault("AI_TRAINING_JDBC_USERNAME", "eshop"), password);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        Long count = transaction.execute(status -> new AiTrainingDataSeeder(new JdbcTemplate(source)).seed(AiTrainingSeedData.DATASET_CODE));
        log.info("AI bootstrap dataset ready: {} examples. Existing rows preserved; no model training started", count);
    }
}

package com.example.eshop.ai.seed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "AI_TRAINING_TEST_URL", matches = ".+")
class AiTrainingDataSeederPostgresTest {
    @Test void seedsOneHundredThousandAndPreservesReviewedExamplesOnRerun() {
        var source = new DriverManagerDataSource(System.getenv("AI_TRAINING_TEST_URL"),
            System.getenv("AI_TRAINING_TEST_USERNAME"), System.getenv("AI_TRAINING_TEST_PASSWORD"));
        var jdbc = new JdbcTemplate(source);
        new TransactionTemplate(new DataSourceTransactionManager(source)).executeWithoutResult(transaction -> {
            transaction.setRollbackOnly();
            String code = "seeder-test-" + UUID.randomUUID();
            var seeder = new AiTrainingDataSeeder(jdbc);
            assertThat(seeder.seed(code)).isEqualTo(100_000);
            UUID dataset = jdbc.queryForObject("SELECT id FROM ai_training.datasets WHERE code=?", UUID.class, code);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_training.examples WHERE dataset_id=? AND review_status='DRAFT'", Long.class, dataset)).isEqualTo(100_000);
            UUID example = jdbc.queryForObject("SELECT id FROM ai_training.examples WHERE dataset_id=? ORDER BY id LIMIT 1", UUID.class, dataset);
            jdbc.update("UPDATE ai_training.examples SET source_verified=true,consent_confirmed=true WHERE id=?", example);
            jdbc.update("UPDATE ai_training.examples SET review_status='APPROVED',reviewed_by='test',reviewed_at=now() WHERE id=?", example);
            assertThat(seeder.seed(code)).isEqualTo(100_000);
            assertThat(jdbc.queryForObject("SELECT review_status FROM ai_training.examples WHERE id=?", String.class, example)).isEqualTo("APPROVED");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_training.approved_examples WHERE dataset_code=?", Long.class, code)).isEqualTo(1);
        });
    }
}

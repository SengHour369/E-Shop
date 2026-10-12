package com.example.eshop.ai.seed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AI_TRAINING_TEST_URL", matches = ".+")
class AiTrainingDataSeederPostgresTest {
    private DriverManagerDataSource source() {
        return new DriverManagerDataSource(System.getenv("AI_TRAINING_TEST_URL"),
            System.getenv("AI_TRAINING_TEST_USERNAME"), System.getenv("AI_TRAINING_TEST_PASSWORD"));
    }
    private void cleanup(JdbcTemplate jdbc, String code) {
        jdbc.update("DELETE FROM ai_training.seed_progress WHERE dataset_id IN (SELECT id FROM ai_training.datasets WHERE code=?)", code);
        jdbc.update("DELETE FROM ai_training.examples WHERE dataset_id IN (SELECT id FROM ai_training.datasets WHERE code=?)", code);
        jdbc.update("DELETE FROM ai_training.datasets WHERE code=?", code);
    }
    @Test void seedsResumesExpandsAndPreservesHumanReview() {
        var jdbc = new JdbcTemplate(source());
        String code = "seeder-test-" + UUID.randomUUID();
        try {
            assertThat(new AiTrainingDataSeeder(jdbc,2000,200).seed(code)).isEqualTo(2000);
            UUID dataset = jdbc.queryForObject("SELECT id FROM ai_training.datasets WHERE code=?", UUID.class,code);
            UUID example = jdbc.queryForObject("SELECT id FROM ai_training.examples WHERE dataset_id=? ORDER BY id LIMIT 1",UUID.class,dataset);
            jdbc.update("UPDATE ai_training.examples SET source_verified=true,consent_confirmed=true WHERE id=?",example);
            jdbc.update("UPDATE ai_training.examples SET review_status='APPROVED',reviewed_by='test',reviewed_at=now() WHERE id=?",example);
            jdbc.update("UPDATE ai_training.seed_progress SET next_index=1800 WHERE dataset_id=?",dataset);
            assertThat(new AiTrainingDataSeeder(jdbc,2000,200).seed(code)).isEqualTo(2000);
            assertThat(new AiTrainingDataSeeder(jdbc,2200,200).seed(code)).isEqualTo(2200);
            assertThat(jdbc.queryForObject("SELECT review_status FROM ai_training.examples WHERE id=?",String.class,example)).isEqualTo("APPROVED");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_training.approved_examples WHERE dataset_code=?",Long.class,code)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_training.examples WHERE dataset_id=? AND language='km'",Long.class,dataset)).isEqualTo(1100);
        } finally { cleanup(jdbc,code); }
    }
    @Test void failureRollsBackCurrentBatchButRetainsCommittedCheckpoint() {
        var source = source();
        var updates = new AtomicInteger();
        var faulty = new JdbcTemplate(source) {
            @Override public int update(String sql,Object... args) {
                if(sql.startsWith("UPDATE ai_training.seed_progress SET next_index=") && updates.incrementAndGet()==2)
                    throw new IllegalStateException("Injected checkpoint failure");
                return super.update(sql,args);
            }
        };
        var jdbc = new JdbcTemplate(source);
        String code = "seeder-failure-" + UUID.randomUUID();
        try {
            assertThatThrownBy(() -> new AiTrainingDataSeeder(faulty,6,2).seed(code)).hasMessage("Injected checkpoint failure");
            UUID dataset=jdbc.queryForObject("SELECT id FROM ai_training.datasets WHERE code=?",UUID.class,code);
            assertThat(jdbc.queryForObject("SELECT next_index FROM ai_training.seed_progress WHERE dataset_id=?",Integer.class,dataset)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_training.examples WHERE dataset_id=?",Long.class,dataset)).isEqualTo(2);
            assertThat(new AiTrainingDataSeeder(jdbc,6,2).seed(code)).isEqualTo(6);
        } finally { cleanup(jdbc,code); }
    }
}

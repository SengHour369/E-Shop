package com.example.eshop.ai.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Objects;
import java.util.UUID;

/** Opt-in seed worker: bounded memory, atomic per-batch checkpoints, no model training. */
@Component
@ConditionalOnProperty(name = "ai.training.seed.enabled", havingValue = "true")
public class AiTrainingDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            AiTrainingDataSeeder.class
    );

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final int target;
    private final int batchSize;

    public AiTrainingDataSeeder(JdbcTemplate jdbc) {
        this(jdbc, AiTrainingSeedData.SIZE, 1000);
    }

    @Autowired
    public AiTrainingDataSeeder(
            JdbcTemplate jdbc,
            @Value("${ai.training.seed.target:100000}") int target,
            @Value("${ai.training.seed.batch-size:1000}") int batchSize
    ) {
        if (target < 2
                || target > AiTrainingSeedData.MAX_SIZE
                || target % 2 != 0) {
            throw new IllegalArgumentException(
                    "Seed target must be even, between 2 and 10,000,000"
            );
        }

        if (batchSize < 2 || batchSize > 10_000 || batchSize % 2 != 0) {
            throw new IllegalArgumentException(
                    "Seed batch size must be even, between 2 and 10,000"
            );
        }

        this.jdbc = jdbc;
        this.target = target;
        this.batchSize = batchSize;
        this.transaction = new TransactionTemplate(
                new DataSourceTransactionManager(
                        Objects.requireNonNull(jdbc.getDataSource())
                )
        );
        this.transaction.setTimeout(90);
    }

    @Override
    public void run(ApplicationArguments arguments) {
        log.info(
                "AI seed ready: {} examples; no model training started",
                seed(AiTrainingSeedData.DATASET_CODE)
        );
    }

    long seed(String code) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Run seed outside a surrounding transaction so batches can commit"
            );
        }

        if (!Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT to_regclass('ai_training.seed_progress') IS NOT NULL",
                Boolean.class
        ))) {
            throw new IllegalStateException(
                    "Apply ai_training migrations 20261011_ai_training.sql "
                            + "and 20261012_ai_training_seed_progress.sql first"
            );
        }

        UUID dataset = transaction.execute(status -> {
            jdbc.execute("SET LOCAL lock_timeout = '5s'");
            jdbc.queryForList("SELECT pg_advisory_xact_lock(731045212)");
            jdbc.update(
                    """
                    INSERT INTO ai_training.datasets(code,name,description,created_by)
                    VALUES (?, ?, 'Synthetic English/Khmer template combinations. Human review required; not independent facts or an evaluation benchmark.', 'ai-training-seeder-v1')
                    ON CONFLICT(code) DO NOTHING
                    """,
                    code,
                    "E-Shop synthetic bootstrap: " + target + " examples"
            );
            UUID id = jdbc.queryForObject(
                    "SELECT id FROM ai_training.datasets WHERE code=?",
                    UUID.class,
                    code
            );

            if (!"ai-training-seeder-v1".equals(
                    jdbc.queryForObject(
                            "SELECT created_by FROM ai_training.datasets WHERE id=?",
                            String.class,
                            id
                    )
            )) {
                throw new IllegalStateException(
                        "Dataset is not owned by this seeder; "
                                + "choose a new dataset code"
                );
            }

            jdbc.update(
                    """
                    INSERT INTO ai_training.seed_progress(dataset_id,generator_version,target_count,next_index)
                    VALUES (?,?,?,0) ON CONFLICT(dataset_id) DO UPDATE
                    SET target_count=GREATEST(ai_training.seed_progress.target_count,excluded.target_count)
                    """,
                    id,
                    AiTrainingSeedData.GENERATOR_VERSION,
                    target
            );
            jdbc.update(
                    """
                    UPDATE ai_training.datasets SET name='E-Shop synthetic bootstrap: ' || p.target_count || ' examples',
                        description='Synthetic English/Khmer template combinations. Human review required; not independent facts or an evaluation benchmark.'
                    FROM ai_training.seed_progress p WHERE ai_training.datasets.id=p.dataset_id AND p.dataset_id=?
                    """,
                    id
            );
            return id;
        });

        int committed;

        do {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException(
                        "Seed interrupted; committed batches can resume"
                );
            }

            committed = Objects.requireNonNull(
                    transaction.execute(status -> seedBatch(dataset))
            );

            if (committed % 100_000 == 0 || committed >= target) {
                log.info(
                        "AI seed checkpoint: {} / {}",
                        committed,
                        target
                );
            }
        } while (committed < target);

        return Objects.requireNonNull(
                jdbc.queryForObject(
                        "SELECT count(*) FROM ai_training.examples "
                                + "WHERE dataset_id=?",
                        Long.class,
                        dataset
                )
        );
    }

    private int seedBatch(UUID dataset) {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        jdbc.execute("SET LOCAL statement_timeout = '60s'");
        var progress = jdbc.queryForMap(
                "SELECT generator_version,next_index FROM "
                        + "ai_training.seed_progress WHERE dataset_id=? FOR UPDATE",
                dataset
        );

        if (!AiTrainingSeedData.GENERATOR_VERSION.equals(
                progress.get("generator_version")
        )) {
            throw new IllegalStateException(
                    "Generator version mismatch; use a new dataset instead "
                            + "of changing existing content"
            );
        }

        int offset = ((Number) progress.get("next_index")).intValue();

        if (offset >= target) {
            return offset;
        }

        int count = Math.min(batchSize, target - offset);
        jdbc.batchUpdate(
                """
                INSERT INTO ai_training.examples(dataset_id,language,prompt,response,split,source,created_by)
                VALUES (?,?,?,?,?,?,'ai-training-seeder-v1')
                ON CONFLICT(dataset_id,language,content_hash) DO NOTHING
                """,
                AiTrainingSeedData.batch(offset, count),
                count,
                (statement, example) -> {
                    statement.setObject(1, dataset);
                    statement.setString(2, example.language());
                    statement.setString(3, example.prompt());
                    statement.setString(4, example.response());
                    statement.setString(5, example.split());
                    statement.setString(
                            6,
                            "synthetic:" + AiTrainingSeedData.GENERATOR_VERSION
                    );
                }
        );
        jdbc.update(
                "UPDATE ai_training.seed_progress "
                        + "SET next_index=?,updated_at=now() WHERE dataset_id=?",
                offset + count,
                dataset
        );
        return offset + count;
    }

    public static void main(String[] arguments) {
        String password = System.getenv("AI_TRAINING_JDBC_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Set AI_TRAINING_JDBC_PASSWORD"
            );
        }

        var source = new DriverManagerDataSource(
                System.getenv().getOrDefault(
                        "AI_TRAINING_JDBC_URL",
                        "jdbc:postgresql://localhost:5437/ai_db"
                ),
                System.getenv().getOrDefault(
                        "AI_TRAINING_JDBC_USERNAME",
                        "eshop"
                ),
                password
        );
        int target = Integer.parseInt(
                System.getenv().getOrDefault(
                        "AI_TRAINING_SEED_TARGET",
                        "10000000"
                )
        );
        int batch = Integer.parseInt(
                System.getenv().getOrDefault(
                        "AI_TRAINING_SEED_BATCH_SIZE",
                        "1000"
                )
        );
        long count = new AiTrainingDataSeeder(
                new JdbcTemplate(source),
                target,
                batch
        ).seed(AiTrainingSeedData.DATASET_CODE);
        log.info(
                "AI dataset ready: {} examples. "
                        + "Existing reviews preserved; no model training started",
                count
        );
    }
}
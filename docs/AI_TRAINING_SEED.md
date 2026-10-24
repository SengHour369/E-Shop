For the current runtime retrieval setup and verification, see [seed-retrieval.md](voice/seed-retrieval.md). Seed storage is separate from model-weight training.

# Runtime AI training seed: 100,000 examples

## Java classes

- `ai-service/src/main/java/com/example/eshop/ai/seed/AiTrainingSeedData.java` generates a deterministic, immutable collection of 100,000 synthetic examples.
- `ai-service/src/main/java/com/example/eshop/ai/seed/AiTrainingDataSeeder.java` implements Spring Boot `ApplicationRunner` and writes batches of 250 rows in one transaction.

Dataset code: `eshop-bootstrap-10000-v1`. This is an additional dataset; the original 12 examples remain untouched.

The corpus has 40 topics, 125 generic product descriptions and 10 phrasing styles in two languages: 50,000 English and 50,000 Khmer examples. The subsets contain 80,000 TRAIN, 12,000 VALIDATION and 8,000 TEST rows. Product descriptions are grouped into subsets; topic and phrasing templates are shared, so these subsets are not an independent quality benchmark.

Topics include product search/comparison/options/pricing, stock, delivery, ordering, payment methods, QR payments, pending payments, unverified payment claims, order tracking/cancellation, refunds, returns, addresses, promotions, notifications, account recovery and privacy. No real customer records, credentials, prices or stock quantities are copied. Expected tools remain empty; these examples teach conversational guidance rather than fabricated tool executions.

All inserted examples start as DRAFT, with source verification and consent unconfirmed. They do not enter the approved export until reviewed. The seeder never updates existing rows, approvals, live business records or model configuration. An advisory transaction lock serializes replicas, and the database uniqueness constraint prevents duplicate insertion on reruns.

## Startup

Apply `ai-service/src/main/resources/db/upgrade/20261011_ai_training.sql` to `ai_db` first. Build/redeploy ai-service with the new classes, then set:

```text
AI_TRAINING_SEED_ENABLED=true
```

Alternatively pass `--ai.training.seed.enabled=true` when starting the Java service. The feature defaults to false; enabling it is intentional. Existing running Docker images need rebuilding before they contain this code. For Compose, pass the property in the ai-service environment or Spring application arguments; a host variable alone does not enter a container unless mapped.

## One-shot execution without restarting services

After building the Spring Boot jar, configure these environment variables in the launching process:

```text
AI_TRAINING_JDBC_URL=jdbc:postgresql://localhost:5437/ai_db
AI_TRAINING_JDBC_USERNAME=eshop
AI_TRAINING_JDBC_PASSWORD=<your-local-ai-db-password>
```

Run from the backend root:

```powershell
java -Xmx512m -Dloader.main=com.example.eshop.ai.seed.AiTrainingDataSeeder -cp ai-service/target/ai-service-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

The one-shot path uses the same generator and transaction-backed insert logic. No AI provider, web authentication, Kafka, or other business services need to be restarted.

## Tests

`AiTrainingSeedDataTest` checks exact size, unique prompts, equal language distribution, deterministic generation, split counts, Khmer Unicode, payment verification rules and privacy guidance.

`AiTrainingDataSeederPostgresTest` is enabled when `AI_TRAINING_TEST_URL`, `AI_TRAINING_TEST_USERNAME` and `AI_TRAINING_TEST_PASSWORD` are configured. It inserts into a unique temporary dataset within a rolled-back transaction, reruns the seeder and checks that an approved example remains approved. It leaves no test dataset behind.

```powershell
mvn -pl ai-service -am test -Dtest=AiTrainingSeedDataTest,AiTrainingDataSeederPostgresTest -Dsurefire.failIfNoSpecifiedTests=false
```

Creating 100,000 template-generated rows is not equivalent to 100,000 independently written examples or a trained model. Human review, deduplication beyond exact matches, independent evaluation, external model training and compatible model deployment remain separate steps. Production order/payment decisions continue to use authoritative services.

The original `eshop-bootstrap-10000-v1` dataset code and source label are retained for compatibility. The corpus has been expanded in place to 100,000 rows; previous examples and reviews are preserved.

Expansion to 100,000 retains the legacy dataset code, prompts, responses and product-group splits of the original 20,000 examples. It adds 100 bilingual product descriptions across the same 40 topic templates. These are synthetic combinations, not 100,000 independently curated answers.

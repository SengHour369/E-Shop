# Verification — 20 October 2026

## Analysis → proposed changes → changed files

Prior backend supported workflow draft CRUD and validation only. Added immutable read-only versions, persisted manual execution, step transitions, duplicate-request protection, auditing and PostgreSQL upgrade constraints by extending the existing AI registry/executor.

Changed/added paths:

- `ai-service/src/main/java/com/example/eshop/ai/workflow/`: new publication policy/service/repository/entity, execution controller, manual runner, ledger, run and step entities/repositories; catalog capability metadata and draft publication lock.
- `ai-service/src/main/resources/db/upgrade/20261020_workflow_manual_execution.sql`.
- `common-lib/src/main/java/com/example/eshop/common/audit/AuditSnapshots.java`: explicitly allow scalar workflow/run/step audit identifiers.
- `ai-service/src/test/java/com/example/eshop/ai/WorkflowLiveExecutionTest.java`.
- `scripts/verify-ai-workflow-live.py`, `scripts/verify-ai-workflow-migrations.py`.
- `docs/ai-automation/`.

## Actual results

Live/persistence command:

```sh
WORKFLOW_LIVE_CATALOG_URL=http://localhost:8082 WORKFLOW_LIVE_PRODUCT_ID=1 \
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
mvn -pl ai-service -am -Dtest=WorkflowDraftIntegrationTest,WorkflowLiveExecutionTest \
-Dsurefire.failIfNoSpecifiedTests=false test
```

**10 tests passed, zero failures/errors/skips; BUILD SUCCESS.** Five tests cover draft authoring and five cover execution/publication with the real runner. Real HTTP calls reached the already running catalog on port 8082. No mocked executor, catalog client, business responses or audit service were used in the live execution tests. Identity is a test fixture; persistence is H2 in PostgreSQL compatibility mode.

Verified behaviors: two real read steps persist response evidence, repeated keys return the original records without new audits, missing product stops the run and skips later steps, published snapshots survive draft changes, stale publication rejects, write tool publication rejects, cross-owner execution rejects, mismatched key reuse rejects and stale reconciliation does not replay interrupted steps.

PostgreSQL command:

```sh
python3 scripts/verify-ai-workflow-migrations.py
```

**5 verification stages passed** on a disposable PostgreSQL 18 container with networking disabled: both migrations apply twice; records persist; published updates/deletes reject; duplicate run IDs and step positions reject; absent-version foreign keys reject. Container removed afterward. No project database was modified.

Smoke runner CLI help executed successfully. Full authenticated gateway smoke has not been executed because no administrator token was provided and the running AI service was not redeployed.

## Corrections during verification

An initial test compilation used `AuditLogRepository.count()` despite the deliberately restricted repository only exposing specification-based count; corrected to its real API and reran successfully. The first disposable PostgreSQL readiness check observed the temporary initialization server; corrected to require TCP readiness before migration execution and reran successfully.

## Remaining risks → next phase

Apply both upgrades and deploy the built AI service before authenticated HTTP testing. Per-tool live authorization is enforced in production, but the live integration tests do not validate the gateway/login boundary. Enable business-write workflows only after independent approval records and precise permission grants are implemented. Add background durable workers/event inbox and scheduling before event-triggered execution. Current synchronous runs stop on errors and do not automatically replay uncertain work.

Final packaging/regression command:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
mvn -pl ai-service -am -Dtest=AuditIntegrationTest,WorkflowDraftIntegrationTest,WorkflowLiveExecutionTest \
-Dsurefire.failIfNoSpecifiedTests=false package
```

**BUILD SUCCESS**, executable AI service JAR produced. Five common audit regression tests and five draft tests passed. In this separate packaging invocation the five opt-in live tests were skipped because the live-catalog environment variable was intentionally absent; their executed/pass result is recorded above from the explicit live-enabled invocation. Python scripts passed `py_compile`; the live smoke CLI `--help` ran successfully.

# Backend AI Automation — manual read-only workflows

The module lives inside the existing `ai-service` at `com.example.eshop.ai.workflow`. It uses the existing JWT filter, live administrator permission checks, registered tool schemas, fixed Feign clients, JPA and append-only audit infrastructure. It does not create a separate demo service.

## Implemented

- Persisted owner-scoped drafts with optimistic revisions and server validation.
- Immutable published snapshots for manual catalog/inventory read workflows; drafts remain editable independently.
- Real sequential tool execution through `AiToolExecutor`, rechecking current permissions and schemas before each call.
- Committed run and step records before/after HTTP calls. No transaction is held across business-tool HTTP calls. Live authorization lookups still occur inside transition transactions.
- UUID idempotency keys bound to actor and published version. A repeated key returns the same execution without replay; conflicting reuse returns 409.
- Actual step status, start/end timestamps, tool identity, response SHA-256 and byte count. Raw responses and bearer credentials are never stored in run/step records.
- Run/step audit correlation with request IDs and published-version IDs.
- Failure stops the run and marks later steps SKIPPED. Missing/uncertain upstream results are UNKNOWN, never synthetic success.
- Explicit stale-run reconciliation after 15 minutes. Interrupted steps become UNKNOWN and pending steps become SKIPPED; no automatic replay.
- PostgreSQL immutable-version trigger, foreign keys and unique execution/step constraints.

Only PRODUCT_GET, PRODUCT_DETAIL, PRODUCT_VARIANTS, SKU_GET, PRODUCT_STOCK_CHECK, PRODUCT_SEARCH, INVENTORY_GET, INVENTORY_LOW_STOCK and PROMOTION_GET may execute. The registry must also permit the actor and validate the arguments. This explicit subset excludes legacy tools whose READ_ONLY classification still permits QR issuance or persisted review alerts.

## API

All routes below have prefix `/api/ai/admin/workflows` and require an authenticated live administrator. Definitions/runs are scoped to their owner.

| Method / suffix | Behavior / body |
| --- | --- |
| GET `/catalog` | Permission-filtered tools and backend capability flags |
| POST `/` | Save draft: `{ "definition": { ... } }` |
| GET `/?page=0&search=...` | Draft list, 20 rows/page with total |
| GET `/{workflowId}` | Saved draft |
| PUT `/{workflowId}` | Save draft with `{ "revision": 0, "definition": { ... } }` |
| POST `/validate` | `{ "definition": { ... } }`; validation only, no handlers called |
| POST `/{workflowId}/publish` | `{ "revision": 0 }`; validates and freezes an executable read-only snapshot |
| GET `/{workflowId}/versions` | Retained immutable published snapshots |
| POST `/versions/{versionId}/execute` | `{ "idempotencyKey": "<new UUID>" }`; real sequential tool calls |
| GET `/executions?page=0` | Owner's persisted execution list with total |
| GET `/executions/{runId}` | Run summary and ordered step timeline |
| POST `/executions/{runId}/reconcile` | Mark an abandoned RUNNING execution UNKNOWN after 15 minutes |

A draft definition contains `name`, `description`, `module`, `triggerType: "MANUAL"` and `steps`. Each step contains unique `key`, `name`, registered `tool` and schema-valid `parameters`. Only literal arguments are supported; template bindings, condition syntax and arbitrary HTTP/SQL are not execution inputs.

Example definition (replace the product ID with an actual resource):

```json
{
  "name": "Catalog read review",
  "description": "Read product details and search the live catalog.",
  "module": "catalog-service",
  "triggerType": "MANUAL",
  "steps": [
    {"key":"read-product","name":"Read product","tool":"PRODUCT_GET","parameters":{"id":1}},
    {"key":"search","name":"Search catalog","tool":"PRODUCT_SEARCH","parameters":{"query":"phone"}}
  ]
}
```

`/validate` is strictly a dry run. `/versions/{id}/execute` is the separate real read-only execution endpoint. HTTP 200 alone does not mean execution succeeded: inspect `run.status` and each `steps[].status`.

## Deployment

Apply the upgrades to the AI database in order using the existing migration process:

1. `ai-service/src/main/resources/db/upgrade/20261020_workflow_drafts.sql`
2. `ai-service/src/main/resources/db/upgrade/20261020_workflow_manual_execution.sql`

Hibernate `ddl-auto=update` does not install the PostgreSQL immutability trigger; apply the SQL explicitly. The migration verification script only touches a disposable container and does not deploy these upgrades into project databases.

Build/restart the existing AI service through your existing environment/secret configuration. Gateway routing already covers `/api/ai/**`; no new public route is required. The running Docker AI service was not replaced during this work.

## Step-by-step verification

Run persistence/validation tests and the opt-in tests against the running local catalog:

```sh
WORKFLOW_LIVE_CATALOG_URL=http://localhost:8082 \
WORKFLOW_LIVE_PRODUCT_ID=1 \
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
mvn -pl ai-service -am \
  -Dtest=WorkflowDraftIntegrationTest,WorkflowLiveExecutionTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The live test uses the real registry, executor, Feign HTTP client, running catalog service, persistence and audit service. No business response or business client is mocked. Identity is an isolated administrator fixture, and run persistence uses H2 in PostgreSQL mode: this is not an authenticated gateway or production PostgreSQL integration test. The older draft-only test uses a mocked permission service for its authorization fixtures.

Verify the actual SQL and PostgreSQL constraints independently:

```sh
python3 scripts/verify-ai-workflow-migrations.py
```

After deployment, run the complete authenticated gateway flow with an administrator token supplied securely through `WORKFLOW_API_TOKEN`:

```sh
python3 scripts/verify-ai-workflow-live.py \
  --base http://localhost:8080 --product-id 1 --query phone \
  --report workflow-live-report.json
```

The script performs registry lookup → draft save → validation-only test → immutable publication → actual tool calls → persisted timeline → duplicate-key verification. It creates retained draft/version/run records and only invokes allowlisted catalog reads. It never prints the token or raw upstream payloads, and it stops on real errors. An administrator token was not supplied in this session, so the authenticated gateway script has not been run.

## Scope still missing

This is a real manual read engine, not the complete master specification. Workflow-specific permission grants, independent approvals, business writes, event/scheduled triggers, condition/loop/binding execution, compensation, cancellation, background workers, automatic retry/recovery, activation policies, AI-generated workflow proposals and frontend execution screens remain future phases. Runs execute synchronously with existing client timeouts. A crash retains RUNNING records until explicit reconciliation. Response payload inspection is intentionally limited to hashes and sizes pending an approved redaction schema.

See `verification.md` for commands, actual outcomes and remaining integration boundaries.

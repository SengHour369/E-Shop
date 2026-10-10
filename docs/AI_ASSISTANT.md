# E-Shop AI assistant

## Architecture and reused components

The existing Java `ai-service` is the orchestrator. The existing Python
`ai-inference` service classifies intent and extracts parameters. Java owns
identity, permissions, validation, execution, confirmations and the final data.
The model receives the allowed tool descriptions and the user's question, not
private domain responses. Final messages use grounded English/Khmer templates.

```text
Angular chat -> API Gateway -> ai-service
                               |-> auth-service: current account + effective grants
                               |-> ai-inference: structured intent only
                               |-> registry -> validation -> fixed Feign client
                               |                           -> domain authorization
                               |                           -> safe result projection
                               |-> Redis: limits, references, confirmation
                               |-> PostgreSQL: execution + audit + optional pgvector
                               |-> existing outbox -> Kafka -> notification-service
```

Inventory and promotions remain inside catalog-service. Returns remain inside
order-service. Existing JWT authentication, function/user/group permissions,
request IDs, trace propagation, immutable audits and notification outbox are
reused. There is no separate AI role system or arbitrary model-generated URL,
SQL or shell execution.

## Tool and permission matrix

| Tools | Required access | Behavior |
| --- | --- | --- |
| PRODUCT_SEARCH, PRODUCT_GET, SKU_GET | Public | Active public products and current backend pricing/availability |
| KNOWLEDGE_SEARCH | Public | Approved policy excerpts and source metadata only |
| ORDER_GET, MY_ORDERS, MY_ORDER_STATUS | Current active account | Owned order, latest 20 orders, latest order |
| MY_PAYMENT_STATUS | Current active account | Owned payment by explicit payment ID |
| MY_RETURNS, MY_NOTIFICATIONS | Current active account | Owned returns or notifications |
| ADMIN_ORDER_LIST | Administrator + ORDER_VIEW_ALL | Latest 20 visible orders; optional exact status/date |
| ADMIN_PAYMENT_LIST | Administrator + PAYMENT_VIEW_ALL | Latest 20 visible payments; optional exact status/date |
| ADMIN_RETURN_LIST | Administrator + RETURN_VIEW_ALL | Latest 20 returns; optional exact status |
| ADMIN_USER_LIST | Administrator + USER_VIEW | Latest 20 accounts; no credentials/contact fields |
| ADMIN_ORDER_SUMMARY | Administrator + REPORT_VIEW | Visible orders grouped by status |
| ADMIN_REVENUE_SUMMARY | Administrator + REPORT_VIEW | Completed payments for an explicit date, grouped by currency |
| ADMIN_AUDIT_LOG | Administrator + AUDIT_VIEW | Latest 20 local AI audits matching explicit requestId |
| INVENTORY_GET, INVENTORY_LOW_STOCK | Administrator + INVENTORY_VIEW | Current inventory; low stock defaults threshold 10, max 20 rows |
| PROMOTION_GET | Administrator + PROMOTION_VIEW | Current promotion details |
| PROMOTION_CREATE | Administrator + PROMOTION_MANAGE | Confirmed DRAFT percentage promotion for one SKU |
| PROMOTION_DISABLE | Administrator + PROMOTION_MANAGE | Confirmed disable; preview also requires PROMOTION_VIEW |
| ORDER_CANCEL | Current active account | Confirmed cancellation of an explicitly identified owned PENDING order |
| PAYMENT_REFUND, ROLE_GRANT | Disabled | Never offered or executed by AI |

An administrator JWT alone does not grant an admin tool. Auth-service checks the
current enabled, active (`ACT`), nondeleted account and the active/nondeleted
function and direct/group grants. Admin tools also require a current ADMIN or
SUPER_ADMIN role. A permission service failure denies execution. Domain
adapters enforce the same permission or ownership checks independently.

Lists are bounded and must not be described as complete totals. Revenue here
means gross completed payments, not net profit or revenue after refunds. Dates
use ISO `YYYY-MM-DD`; `TODAY` resolves in Asia/Bangkok. Payment/order timestamps
follow the existing services' local-time storage convention.

## HTTP contracts

Customer/guest chat: `POST /api/ai/chat`.
Administrator chat: `POST /api/ai/admin/chat`.

```json
{
  "message": "Show my latest order",
  "conversationId": null,
  "language": "en"
}
```

Language is `en` or `km`. Response fields are `conversationId`, server-selected
`mode`, `cardType`, `suggestions`, and `result`. The result contains execution,
request and trace IDs, intent, status, message, errorCode and safe data. Never
send userId, roles, mode, permission lists or an endpoint from the frontend.
Unknown request fields are rejected by ai-service.

Guests can search products and public knowledge. A supplied invalid credential
is rejected instead of downgrading to a guest. The gateway converts an existing
login cookie into the authenticated downstream header and retains its existing
Origin checks on unsafe cookie-authenticated calls.

The existing authenticated `POST /api/ai/execute` remains supported, accepting
`{"message":"..."}` and an optional UUID `Idempotency-Key` header.
`GET /api/ai/executions/{id}` is restricted to its owner.

Statuses: SUCCESS=200, RUNNING=202, NEEDS_INPUT=422, DENIED=403,
UNKNOWN=409, FAILURE=502. Validation, login, limits and infrastructure can also
return ordinary HTTP errors. Clients must inspect a structured 422 response:
it can contain a confirmation card rather than a transport failure.

## Write confirmation and uncertain outcomes

A write request validates intent and parameters, checks permissions, fetches
available preview information, and returns a `ConfirmationCard`. It performs
no domain mutation at that stage. The card includes `confirmationId`, tool,
parameters and a 300-second expiry; some tools include a preview.

After an explicit button click, send `POST /api/ai/confirm/{confirmationId}`
with an empty JSON body using the current login. Redis atomically consumes the
actor-bound confirmation. Java rechecks the live grant and parameters, then
calls the existing domain transaction. Confirmation never calls the model.
The same confirmation ID also identifies the persisted execution, preventing
repeated dispatch after a duplicate request. A different actor cannot consume
it. A domain timeout may mean the action already occurred: UNKNOWN responses
require checking backend state, not automatically issuing another mutation.
An interrupted RUNNING execution needs reconciliation. There is no automatic
cross-service rollback or exactly-once claim.

Redis also enforces 20 AI requests/minute per authenticated actor or actual
remote address for guests. It stores only reference context for authenticated
conversations (30-minute TTL), not price/stock snapshots or full chat history.
Guests receive no retained conversation context. When the gateway is a proxy,
guests can share a remote-address bucket; configure trusted identity forwarding
before replacing this with a per-client network limit.

## Optional approved knowledge

Knowledge indexing is off by default. It uses the existing ai_db plus pgvector
and an operator-configured Ollama embedding endpoint. Live business data always
uses domain tools, not knowledge chunks. Retrieval filters by approved state
and the configured embedding model, requires cosine similarity >= 0.75, and
returns at most three excerpts. No reliable match returns an explicit fallback.

An administrator with KNOWLEDGE_MANAGE can directly manage reviewed public text:

- `POST /api/ai/knowledge`: title, language (`en`/`km`), content.
- `PUT /api/ai/knowledge/{documentId}`: replace the approved content atomically.
- `DELETE /api/ai/knowledge/{documentId}`: withdraw all chunks immediately.

Content is limited to 20,000 characters, chunked at 800 with 100 overlap. Store
only public policies, FAQ/help text and reviewed descriptions. These explicit
management APIs are not model tools. Reindex after changing embedding models;
do not mix vector dimensions within the same model name. Small-corpus retrieval
uses an exact vector scan; production scale needs a dimension-specific index.
Document replacement/removal is audited without retaining full content in the
audit. Historical knowledge versions and a separate approval workflow are not
implemented.

## Configuration and migrations

The project uses manual `src/main/resources/db/upgrade` migrations, not an
installed migration runner. Back up databases and apply scripts through the
existing release procedure. Do not rely on Hibernate `update` in production.

1. Apply the existing AI orchestration/audit/outbox migrations already required
   by the project, including ai-service's `20261003_ai_orchestration.sql`.
2. Apply auth-service's `20261008_ai_function_permissions.sql`. It inserts only
   missing function definitions and grants nobody implicitly. Assign permissions
   through the existing user/group administration flow.
3. If enabling knowledge, first install pgvector binaries compatible with the
   existing PostgreSQL server, then apply ai-service's
   `20261008_ai_knowledge.sql` as the migration owner. The stock postgres:18
   image in compose.ai.yaml does not supply pgvector binaries. Keep knowledge
   disabled until the extension and tables are ready.

Environment settings:

| Setting | Purpose |
| --- | --- |
| AUTHORIZATION_AUTH_URL | Fixed auth-service URL; localhost:8081 locally, auth-service:8081 in Compose |
| AI_PROVIDER | OPENAI (existing provider) or OLLAMA |
| OPENAI_API_KEY / OPENAI_MODEL | Existing OpenAI provider settings |
| OLLAMA_BASE_URL / OLLAMA_MODEL | Fixed local provider server and explicitly installed model |
| AI_KNOWLEDGE_ENABLED | false until the optional vector store is ready |
| AI_KNOWLEDGE_EMBEDDING_URL / AI_KNOWLEDGE_EMBEDDING_MODEL | Fixed embedding server and explicitly installed model |
| REDIS_HOST / REDIS_PORT | Existing Redis for limits, context and confirmation |
| JWT_SECRET / EUREKA_URL / database and Kafka settings | Existing shared service settings |

The Ollama adapter uses structured `format`, `stream:false` and separate system
and user messages at `/api/chat`; knowledge uses `/api/embed`. See the official
[chat API](https://docs.ollama.com/api/chat) and
[embedding API](https://docs.ollama.com/api/embed). Vision requests remain
unsupported by this provider. No models are installed by the application.

Build with `mvn -DskipTests package` from the root. For development, run
`docker compose -f compose.yaml -f compose.ai.yaml up -d --build` after setting
existing credentials and the selected provider. This implementation has not
started containers or changed a running database.

## Angular integration

No Angular project was available in either workspace. The standalone component,
service, template and styles are in `integrations/angular`. Copy them into the
existing app, keep its existing HttpClient/auth interceptor or cookie session,
and register the standalone component in a customer page:

```html
<app-ai-chat></app-ai-chat>
```

Use `<app-ai-chat [administrator]="true"></app-ai-chat>` on the existing guarded
admin page. That input selects the endpoint; the backend still checks admin
identity and grants. Register HttpClient using the app's normal setup and keep
API calls pointed at the gateway. Cross-origin cookie deployments additionally
need the existing credentials/CORS/session configuration. The template escapes
text and shows product/status/admin details, source metadata, loading/errors,
request IDs and an explicit confirmation button. It does not render raw HTML.

## Verification and remaining scope

Targeted Java tests cover identity propagation, unknown/injected intent and
parameters, missing/expired identity, cross-user order/payment access, inactive
accounts, active direct/group grants, missing/revoked permissions, guest limits
on tools, confirmation ownership/single use, idempotency, audit/outbox, provider
failure, and gateway guest/cookie/invalid-credential behavior. Python tests
cover existing inference behavior and the new Ollama structured adapter.

The Java HTTP-loopback tests initially could not start because the Windows
sandbox temporary path was too long for the JDK's internal Unix-domain sockets.
A short `jdk.net.unixdomain.tmpdir` resolved that error without altering tests.
The parent Maven configuration now activates `windows-test-sockets` on Windows
and points test sockets at the module's existing target directory. For a deeply
nested checkout, override `ai.test.socket-directory` with a short existing
directory. This setting affects test JVMs, not deployed services. Run the full
service integration suite before release. Angular compilation and live PostgreSQL/pgvector, Redis, Kafka,
provider/model and full-stack deployment were not verified here.

This is an implemented backend and an integration-ready UI, not a claim that
the entire attached production specification is complete. Advanced product
attribute/price filters, guest follow-up memory, arbitrary report date ranges,
refund execution, account/role writes, broader admin CRUD, cross-service audit
aggregation, PDF ingestion, knowledge version history, semantic caching,
full bilingual clarification templates and production quality/load evaluations
remain outside this implemented tool set. Existing product search matches
active names; it does not promise color/size/budget filtering.

### Latest recorded checks

- Java core: 39 tests passed across auth, order, payment and AI modules, including five HTTP-loopback tests.
- Gateway: 9 tests passed, including public chat with login cookies and invalid credentials.
- Python inference: 48 tests passed.
- `git diff --check`: no whitespace errors.

Reproduce the targeted Java checks with:

```powershell
mvn -pl ai-service,auth-service,catalog-service,order-service,payment-service -am '-Dtest=AiOrchestrationTest,AiConfirmationTest,AiOrderOwnershipTest,AiPaymentSecurityTest,AiPermissionRepositoryTest,AiAccountAuthorizationTest,AiRevenueRepositoryTest,AiInferenceClientTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
mvn -pl api-gateway -am '-Dtest=GatewayAuthenticationFilterTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Run `pytest -q` from ai-inference using its existing dependencies. These targeted
commands intentionally do not stand in for the full repository suite or a live
provider/deployment test.

### Requested rerun — 2026-10-09

The selected Java run included the five HTTP-loopback inference tests this time:
43 tests passed and five errored with `Unable to establish loopback connection`.
The combined Java run therefore ended in BUILD FAILURE. Retrying just those
five with broader local access produced the same loopback errors. There were
no assertion failures. Python completed with 48 passed and one deprecation
warning. This was automated testing, not a live OpenAI/Ollama deployment test.

Rerun logs in the repository root:
- `ai-assistant-rerun-java.log`
- `ai-assistant-rerun-network.log`
- `ai-assistant-rerun-python.log`

### Resolved Windows socket failure — 2026-10-09

The JDK's internal Unix-domain socket path exceeded the Windows limit because
the sandbox TEMP directory was long. A short `jdk.net.unixdomain.tmpdir` fixed
the issue. No tests were removed, skipped, or weakened.

The final combined selected run passed all 48 Java tests with no failures,
errors, or skips. The final Python run passed all 48 tests with one deprecation
warning. That is 96/96 selected tests passing, not a statement of 100% code
coverage or a live deployment/model guarantee.

The Windows-only Maven profile was independently verified: all five HTTP tests
passed without a manual socket-path launcher flag. The profile uses the
module's existing target directory and can be overridden with
`-Dai.test.socket-directory=<short-existing-directory>`.

Final logs:
- `ai-assistant-all-selected-java.log`
- `ai-assistant-all-python.log`
- `ai-assistant-windows-profile.log`

# E-Shop assistant repository analysis and development plan

Inspection: 2026-10-16. Backend: `/home/senghour/Desktop/E-Shop`. Angular: `/home/senghour/Desktop/Order_Shop_Frontend`. This records source-code findings, not certification of production payment settlement or every deployed environment. Module manifests, controller/service/model boundaries, relevant tests, security, inference, Angular clients, and infrastructure configuration were inspected. Existing unrelated local edits are preserved.

## Existing implementation

| Area | Implemented in source | Evidence |
|---|---|---|
| AI orchestration | Registry of fixed enum tools; permission-filtered tool catalog; structured provider inference; confidence threshold; schema checks; fixed Feign dispatch; confirmation for every write; execution history, metrics and failure states | `ai-service/src/main/java/com/example/eshop/ai/registry/AiToolRegistry.java`, `service/AiRouterService.java`, `service/AiExecutionService.java`, `service/AiToolExecutor.java` |
| Inference | Separate FastAPI service with Gemini/OpenAI/Ollama intent providers; English/Khmer/mixed-language system instructions; structured decisions restricted to supplied tools; exact English read shortcuts | `ai-inference/app/services/intent_service.py`, `prompt_service.py`, `app/tools/registry.py`, `app/providers/` |
| Conversations | Authenticated Redis references keyed by actor and server conversation UUID; 30-minute TTL; bounded conversational turns; five-minute owner-bound one-use confirmations; rate limiting | `ai-service/.../service/AiChatStateService.java`, `AiConfirmationService.java` |
| Angular | Customer floating assistant, product/result cards, chat checkout, auth-aware state reset, confirmation/retry controls, admin conversations/operations/imports, quota/feedback clients | `src/app/features/assistant/customer-chat/`, `chat-checkout/`, `assistant.api.ts`, `src/app/features/admin/assistant/` in Angular repo |
| Catalog | Product/SKU attributes and pricing; images; promotions; stock visibility; safe AI product/SKU/inventory projections | `catalog-service/.../model/Product.java`, `ProductSku.java`, `controller/AiCatalogController.java` |
| Inventory | Quantity, reserved/available stock, movements and low-stock thresholds; quote/reserve/confirm/release workflows. Inventory is part of catalog-service, not a separate deployable service | `catalog-service/.../model/Inventory.java`, `controller/InventoryController.java`, `CatalogCheckoutController.java`, `service/impl/CatalogCheckoutService.java` |
| Cart | Cart/item CRUD in order-service, ownership checks using CurrentCustomer, SKU-based pricing from catalog, locked cart state | `order-service/.../controller/CartController.java`, `service/impl/CartServiceImpl.java` |
| Orders | Owned order listing/status and confirmed cancellation; admin bounded lists/status summary; idempotent checkout coordinator with reservations and recovery; returns/refunds controllers | `order-service/.../controller/AiOrderController.java`, `AiReturnController.java`, `automation/CommerceOrderController.java`, `service/impl/CheckoutCoordinator.java` |
| Payments | Owned/admin status projections; completed-payment currency summaries; Bakong transaction check, dynamic KHQR generation and verification; automation handles BAKONG/KHQR/ABA through Bakong KHQR. This does NOT prove a standalone ABA PayWay/acquiring integration | `payment-service/.../controller/AiPaymentController.java`, `bakong/`, `automation/PaymentAutomation.java`, `PaymentAutomationController.java` |
| Notifications | Owned inbox/read state/preferences; Kafka event consumption with event-ID deduplication; AI execution outcomes published through a transactional outbox | `notification-service/.../controller/NotificationController.java`, `consumer/NotificationEventConsumer.java`; `ai-service/.../service/NotificationOutboxPublisher.java` |
| Security | JWT signature/expiry/enabled-token validation; CurrentActor identity; live account/permission snapshot from auth; domain ownership checks; admin function checks; disabled sensitive tools | `common-lib/.../security/`, `common-lib/.../jwt/`, `auth-service/.../controller/InternalAuthorizationController.java`, domain AI controllers |
| Infrastructure | Eureka discovery, fixed/internal Feign clients, Redis context/confirmations/rate limits, PostgreSQL service stores, Kafka outbox delivery, gateway route/auth/request tracing and circuit breakers | `compose.yaml`, `compose.ai.yaml`, per-service `application.yml`, `common-lib/.../request/` |

Paths abbreviated `module/.../` mean `module/src/main/java/com/example/eshop/<module package>/`.

## Existing APIs the assistant can reuse

| Use | Approved existing API | Access / limitations |
|---|---|---|
| Owned order | GET `/internal/ai/orders/{number}`, `/internal/ai/orders`, `/internal/ai/orders/latest` | Current live account; owner resolved from JWT |
| Cancel order | GET `/internal/ai/orders/{number}/cancel-preview`; POST `/internal/ai/orders/{number}/cancel` | Owner; pending-order eligibility; explicit AI confirmation |
| Inventory | GET `/internal/ai/inventory/{skuId}`, `/internal/ai/inventory/low-stock` | Administrator + INVENTORY_VIEW; bounded page |
| Owned payment | GET `/internal/ai/payments/{id}` | Owner; no QR/provider credentials in response |
| Admin operations | GET `/internal/ai/orders/admin`, `/orders/summary`, `/internal/ai/payments/admin`, `/payments/revenue`, `/internal/ai/returns/admin`, `/internal/ai/users` | Live administrator + tool-specific permission |
| Own returns/notifications | GET `/internal/ai/returns`, `/api/notifications` | Current actor ownership; bounded list |
| Promotions | GET `/internal/ai/promotions/{id}`; POST `/internal/ai/promotions`, `/internal/ai/promotions/{id}/disable` | Permission checks; writes require confirmation; creation is draft only |
| Policies and audit lookup | Existing local AiKnowledgeService and AiAuditLookupService | Approved knowledge; audit requires explicit request ID and permission |
| Product discovery | GET `/internal/ai/products`, `/internal/ai/products/{id}`, `/internal/ai/skus/{id}` | Already registered; no search extension in this phase |
| Cart and checkout | Existing `/api/v1/cart/...`, POST `/api/v1/orders/user/from-cart` (see OrderController), catalog reservation APIs | Already used by Angular checkout; NOT added to the AI tool registry in this phase |
| Payment QR | GET `/api/v1/payments/orders/{id}/qr`, `/qr/image`; POST `/verify` | Ownership + configured merchant/provider; NOT new financial write tools |

The model never receives user-selected URLs, bearer credentials or SQL access. Tool clients target fixed endpoints, forward the caller bearer and propagate request/trace IDs. Source availability does not establish production readiness.

## Gaps and security findings

1. Guest conversation IDs are caller-supplied and guest dialogue uses a shared Redis namespace. A disclosed ID could permit cross-guest history reuse. Make guest conversations stateless now; future persistent guest sessions require an opaque, signed/session-bound server identity.
2. AiQuotaService already fingerprints and caches HTTP request responses, but cached results bypass work-time live authorization. Revalidate current account and the stored tool permission before returning cached chat/execute/confirmation results.
3. AiExecution rows have owner binding but no payload/operation fingerprint. A UUID reused across execute and confirm can replay the wrong accepted operation. Bind execution keys to SHA-256 of operation + request. Legacy unfingerprinted rows must fail closed for routing, while remaining readable as history.
4. Router response checks do not reject all existing envelope representations (`success:false`, textual error codes, textual HTTP statuses). Reject these without returning provider/downstream internals.
5. English/Khmer provider instructions already exist, but deterministic shortcuts cover English only. Add exact Khmer/English read phrases; ambiguous or write instructions continue through inference, validation and confirmation. Do not promise arbitrary natural-language accuracy.
6. A confirmed write is at most once per confirmation execution. A remote write followed by a timeout or local crash remains UNKNOWN/RUNNING. There is no universal downstream idempotency or distributed transaction for every registered write. Do not retry unknown writes automatically; broader durable reconciliation is a future phase.
7. Admin-only operations currently intentionally require `snapshot.administrator()` plus permission. Delegating tools to manager/staff roles requires an explicit business policy and domain changes; do not silently relax this.
8. Several legacy APIs use broad authenticated access and UserMapper includes a password field in its response. Frontend stripping is not a server-side guarantee. Keep these legacy controllers outside AI dispatch; remove credential fields and audit legacy endpoint authorization separately.
9. Shared symmetric JWT secrets/defaults, exposed infrastructure/service ports, secrets in historical example configuration, and missing service-to-service mTLS/asymmetric trust isolation need deployment hardening. Never expose raw credentials or send them to inference.
10. Payment conversation prompt contains provider-availability wording that differs from newer checkout/automation code. Future payment UX must derive availability from an authoritative API. Financial settlement, automatic refunds and role grants remain excluded.
11. No generic chatbot cart-write or full admin-operation tool catalog exists. Some admin UI workflows call ordinary APIs directly; they are not equivalent to registered AI tools. Existing product search and Angular chat checkout remain intact but are not expanded.
12. Redis dialogue expires, but PostgreSQL execution/audit records and quota response caches have separate retention. A production retention/deletion policy is still required; the conversation TTL does not delete those records.

## Development plan and dependencies

### Phase 1: safe routing infrastructure (this implementation)

| Order | File(s) | Change | Dependency |
|---|---|---|---|
| 1 | `ai-service/.../registry/AiToolRegistry.java`, `service/AiRouterService.java` | Reauthorize replayed tool results, reject error envelopes, keep fixed dispatch/schema enforcement | LivePermissionService + existing registry |
| 2 | `ai-service/.../model/AiExecution.java`, `service/AiExecutionService.java`, `service/AiRouterService.java`; `db/migrations/20261016_ai_execution_fingerprint.sql` | Bind execution UUID to payload/operation fingerprint; reject conflicting replay; preserve owner/audit checks | PostgreSQL nullable-column migration before deployment |
| 3 | `ai-service/.../controller/AiController.java` | Live account authorization before quota/cache access; reauthorize cached business results | Steps 1–2; existing AiQuotaService |
| 4 | `ai-service/.../service/AiChatStateService.java` | Stateless guests; retain authenticated actor-bound references and TTL | Existing Redis |
| 5 | `ai-inference/app/services/intent_service.py` | Deterministic Khmer and English approved read intents | Existing structured schemas and allowlist |
| 6 | AI Java/Python regression tests | JWT/revocation/replay isolation, conflict, response errors, guest isolation, bilingual detection, no unauthorized client calls | Steps 1–5 |
| 7 | Existing Maven reactor and Angular build; inference tests | Validate integration without changing product search or checkout | Java 17, Maven, supported Node, Python test dependencies |

No new public API, paid provider, Angular component, dependency library, product-search tool or checkout tool is necessary for phase 1. Existing chat/execute/confirm contracts remain; replay conflicts are explicit errors.

### Subsequent phases (not implemented here)

- Add typed cart read/write AI adapters to order-service after ownership and quantity/price/idempotency contracts are specified; register each operation independently with confirmation for writes.
- Add authoritative payment-method availability API and client; reconcile provider configuration with user-facing messages. Dedicated ABA PayWay requires merchant/provider requirements, callbacks, signature validation and settlement tests.
- Add durable downstream idempotency and reconciliation for each non-idempotent domain write before offering additional admin operations.
- Expand tool-specific schemas (enums, bounded filters and typed DTOs), approved projections and operator configuration; never add an arbitrary HTTP executor.
- Add persistent guest sessions only with server-issued identity binding, retention/deletion controls and privacy policy.
- Extend existing Angular result cards only when corresponding backend capabilities exist; do not implement simulated success flows.
- Apply server-side credential redaction, stricter legacy role/ownership authorization and deployment secret/trust isolation as separately reviewed work.

## Phase 1 implementation and verification

Implemented the phase 1 file changes above and regression tests in `AiOrchestrationTest.java`, `AiExecutionFingerprintTest.java`, `AiGuestConversationIsolationTest.java`, `AiRegistryBoundaryTest.java`, and `ai-inference/tests/test_intent_service.py`. Registry validation now resolves canonical schemas, rejects disabled tools and non-finite percentages. Existing fixed Feign dispatch, JWT/role/permission checks, domain ownership enforcement, request tracing, audit/outbox and explicit write confirmations are reused.

- Full 11-module Maven reactor package build passed with tests skipped in that build; relevant tests ran separately.
- AI/order/payment test selection: 104 passed and 10 PostgreSQL-specific tests skipped, including the two additional registry-boundary tests. No test failures. The skipped tests are not claimed as verified against PostgreSQL.
- Python inference suite: 88 passed; one dependency deprecation warning.
- Angular assistant suites: 53 passed across 9 files.
- Angular production build passed; initial bundle is 679.50 kB against a 650 kB warning budget.
- `git diff --check` passed.

Deployment prerequisite: apply `db/migrations/20261016_ai_execution_fingerprint.sql` to the AI database before deploying the changed service. It was not applied to a running database in this task. Old execution rows remain available as history, but routing replay requires a new execution key. Services were not restarted or deployed. Tests use mocked provider/downstream responses where appropriate; live settlement, external provider availability and universal distributed-write idempotency are not certified by these tests.

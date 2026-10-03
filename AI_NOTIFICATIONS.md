# AI orchestration and notifications

## Architecture and implementation decisions

The existing Spring Boot 3.5 / Java 17 reactor uses Eureka, OpenFeign, Spring Cloud Gateway,
PostgreSQL per domain, and common-lib security/audit/request-ID helpers. No AI provider,
Kafka broker, or notification service existed. Promotions remain in catalog-service.

New modules:
- ai-service: intent extraction, immutable Java tool registry, validation/authorization,
  fixed Feign calls, execution metadata, audit, and transactional notification outbox.
- notification-service: Kafka consumer, unique event deduplication, owner-scoped in-app APIs,
  opt-in email delivery and retry state.

Auth retains SMTP and OTP delivery. A service-authenticated internal email endpoint looks up
the recipient by user ID and uses existing EmailNotificationService. No email address,
arbitrary subject/body, token, prompt, or model explanation is carried by an event.
There is no separate promotion-service or config-server.

## Start locally

Configure OPENAI_API_KEY and optionally OPENAI_MODEL in your private .env (never commit it).
The configurable default model is gpt-4.1-mini. The fixed OpenAI Responses endpoint uses
strict structured output, store=false, a 3-second connection deadline and 20-second request
deadline. See [OpenAI structured outputs](https://developers.openai.com/api/docs/guides/structured-outputs).

Run:
~~~powershell
docker compose -f compose.yaml -f compose.ai.yaml -f compose.local.yaml up -d --build
~~~
compose.local.yaml retains local HTTP cookies and Mailpit. Omit it for an environment with
real SMTP and HTTPS configuration. AI is reachable through gateway port 8080; notification
development access is also bound to loopback port 8086. New PostgreSQL ports: 5437/5438.
Kafka is published on loopback 9092 and uses kafka:19092 inside Compose.

Without an API key the service starts but returns AI_NOT_CONFIGURED, with execution history
and a failure audit. There is no production mock or pretend AI fallback. Tests replace the
provider with explicit mocks and do not spend provider credits.

## APIs

All new user APIs require a valid enabled access JWT with a numeric userId from auth-service.
Frontend userId, roles, permissions, and extra AI request fields are rejected. JWT authority
checks follow current domain conventions: ADMIN for inventory and promotion administration,
authenticated users for product reads and their own order. Auth's function-ID permission
tables are local to auth; this implementation does not invent mappings for those IDs.

POST /api/ai/execute
Headers: Authorization: Bearer <access token>, X-Request-ID (optional),
Idempotency-Key: <UUID> (strongly recommended for writes).
Body:
~~~json
{"message":"Show product SKU 100"}
~~~

A promotion requires explicit name, SKU, percentage, startAt, and endAt:
~~~json
{"message":"Create a draft promotion named Weekend Sale with 20 percent discount for SKU 501, starting 2026-11-01T00:00:00 and ending 2026-11-02T00:00:00"}
~~~

Dates use catalog's existing LocalDateTime convention. Creation and SKU assignment are
one catalog transaction and leave the promotion DRAFT. No activation occurs.

Responses include executionId, requestId, traceId, intent, status, safe message/errorCode,
and data on success. Status codes: 200 success, 202 existing running request, 422 clarify
missing/invalid/uncertain input, 403 denied, 502 provider/downstream failure, 409 uncertain
write outcome. No model-generated URL is ever used.

GET /api/ai/executions/{UUID} returns only the caller's execution metadata. Reusing an
Idempotency-Key returns the saved status without executing again or returning cached
business data. The same key must represent the same logical request; a new request needs
a new key. Keys are never reused by another user. No automatic mutation retries occur.

A timeout or process interruption may leave the downstream write committed. UNKNOWN or
stale RUNNING requires checking the catalog's audit records by requestId before issuing a
new key. There is no distributed transaction between AI and catalog. The durable acceptance
record is written before dispatch; business audit in catalog is authoritative for a write.

## Registry

| Tool | Permission | Risk | Operation |
|---|---|---|---|
| PRODUCT_GET | authenticated | read only | catalog active product adapter |
| PRODUCT_SEARCH | authenticated | read only | existing paginated product search |
| SKU_GET | authenticated | read only | catalog active SKU adapter |
| INVENTORY_GET | ADMIN | read only | existing inventory-by-SKU endpoint |
| INVENTORY_LOW_STOCK | ADMIN | read only | existing low-stock endpoint, bounded page |
| ORDER_GET | owner | read only | order adapter checks owner before business response |
| PROMOTION_GET | ADMIN | read only | existing admin promotion lookup |
| PROMOTION_CREATE | ADMIN | low-risk write | atomic draft creation and SKU assignment |
| ORDER_CANCEL / PAYMENT_REFUND / ROLE_GRANT | unavailable | high-risk write | never dispatched |

The provider receives only enabled tools allowed for the authenticated actor. Registry,
risk, and parameter checks repeat after inference because provider output is untrusted.
Unknown keys, non-integral IDs, out-of-range percentages, malformed dates, low confidence,
and URL-like order IDs cannot reach clients. No business result is sent back to the model.
High-risk confirmation is deliberately not implemented: those tools remain disabled.

## Notification APIs

- GET /api/notifications?page=1&size=20&read=false
  Optional status (SENT/READ), type (AI_ACTION_COMPLETED/AI_ACTION_FAILED),
  read, from, to (UTC ISO instants). Maximum page size 100.
- GET /api/notifications/{id}
- GET /api/notifications/unread-count
- PATCH /api/notifications/{id}/read
- PATCH /api/notifications/read-all
- GET /api/notifications/preferences
- PATCH /api/notifications/preferences with {"aiEmail":true}

All queries/updates constrain userId. Admin status does not grant another user's inbox.
Unread count is a database count. Read updates are idempotent. In-app channel is IN_APP,
with status SENT then READ. Email has separate DISABLED/PENDING/SENT/FAILED state so email
failures cannot hide an in-app notification. Email is off by default.

## Events and delivery

notification.events.v1 carries version 1 NotificationEvent:
eventId, requestId, traceId, spanId, aiExecutionId, userId, type, resourceType, resourceId,
occurredAt. IDs serve different purposes. Only AI write completion/failure produces events;
read requests do not flood inboxes. Existing domain order/payment event production was not
present and is not fabricated; the versioned contract can be extended deliberately later.

The AI result audit, execution status, and outbox insert commit together. The scheduled
publisher waits for broker acknowledgement and retries pending rows with bounded backoff.
The consumer creates one notification per unique eventId. A duplicate is acknowledged only
after the original row is visible. Invalid events go to notification.events.v1.DLT; transient
failures retry three times before dead-letter publication. A failed DLT send must not commit
the original offset. Monitor pending outbox age, FAILED email rows, and DLT offsets.

Email worker retries at most five times with increasing delay, checking the current
preference before dispatch. Auth records successful event IDs to suppress acknowledged
redelivery. SMTP cannot guarantee exactly-once delivery across a crash between SMTP
acceptance and database commit; duplicates are possible in that narrow window.

## Correlation and audit

Existing X-Request-ID handling is reused for gateway, servlet, Feign, and events.
A validated W3C traceparent passes through Feign and supplies passive trace correlation.
This does not create spans or install an exporter. Existing instrumentation may supply
traceId/spanId; event correlation is retained and consumer MDC is restored after handling.
Provider calls do not receive the user's bearer token or identity.

AI_REQUEST_ACCEPTED records durable acceptance, and AI_TOOL_EXECUTION records the final
outcome. History/audit do not store prompts, extracted parameters, model chain-of-thought,
tokens, or downstream response bodies. Audit metadata is allowlisted.
Gateway admin audit routes: /api/admin/audit-logs/ai and /api/admin/audit-logs/notification.

## Databases and production deployment

Development follows existing ddl-auto=update. For production, apply scripts under each
service's src/main/resources/db/upgrade and then set DDL_AUTO=validate for new services.
Apply 20261002_audit_logs.sql in both new databases to enforce append-only audit records.
Apply 20261003_ai_orchestration.sql, 20261003_notifications.sql, and auth's
20261003_notification_mail.sql in their respective databases. Use a separate migration
owner and limit the application's audit table rights to SELECT/INSERT.

Indexes support owner/time, unread, request-ID, pending outbox, and pending email queries.
Each new service owns its database. User IDs are references across services, not cross-DB
foreign keys. Schedule retention for completed execution/outbox/delivery metadata according
to your audit policy; no automatic deletion of audit records is introduced.

Compose is a local development topology: one Kafka broker, plaintext internal traffic,
replication factor 1, and existing development database/JWT defaults. Production must use
secret-managed credentials, Kafka TLS/SASL and topic ACLs (AI publish, notification consume
and DLT publish), replicated brokers/topics, and restricted service network access.
The shared JWT signing arrangement is inherited from this project; separate asymmetric
service identities are a future security migration, not silently introduced here.

## Verification

Run the full suite:
~~~powershell
mvn verify
~~~
Tests cover authorization, schema validation, disabled high-risk tools, injection arguments,
provider failure, idempotency, execution audit/outbox, ownership, pagination, count/read,
consumer redelivery/context cleanup, email success/retry/opt-out, and Feign correlation.
Live OpenAI inference requires a configured API key. Unit/integration provider mocks do not
verify model availability or language accuracy.

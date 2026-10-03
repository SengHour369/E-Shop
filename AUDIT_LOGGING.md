# Audit logging and request correlation

## Existing architecture and decisions

E-Shop uses Spring Boot 3.5, a reactive Spring Cloud Gateway, and four servlet/JPA
domain services with separate PostgreSQL databases. The shared common-lib owns
JWT validation, response envelopes and exception handling. Auth has a custom
UserDetailsImpl principal; other services authenticate verified JWT claims.
BaseEntity supplies lifecycle timestamps only. Existing inventory StockMovement
and gateway request completion tables remain in use.

There was no Micrometer tracing bridge/exporter, OpenTelemetry, Zipkin, Kafka,
business audit table or service HTTP correlation interceptor configured. This
change does not add tracing or a message broker. Existing traceId/spanId MDC
values will appear alongside requestId if tracing is configured later.

Storage is **service-owned**. A central service would add availability dependencies;
Kafka/outbox delivery would require a broker, consumer, deduplication, retry and
retention infrastructure absent here. Local transactional writes provide atomic
durability without that delivery gap. Search each service by the same request ID.
Gateway request completion logs are operational access logs, not business audit
records.

## Request ID lifecycle

- X-Request-ID is the preferred header. Accept one value matching
  [A-Za-z0-9._:-]{1,128}; replace malformed, blank, oversized or repeated values.
- Gateway CorrelationIdFilter is now a WebFilter, covering routed requests and
  local errors. It accepts the legacy X-Correlation-Id only when X-Request-ID is
  absent, and returns/forwards both headers with the same value. Existing gateway
  correlation_id rows/indexes store this value without a schema rename.
- Servlet RequestIdFilter runs before security, returns X-Request-ID and binds
  MDC plus request metadata until the filter chain exits. Cleanup runs in finally.
  Async/error redispatches reuse the request attribute.
- Feign replaces any stale outgoing header with the current value. Payment's
  RestClient uses the Boot builder and shared interceptor.
- TaskDecorator copies MDC for Boot-managed executor tasks and restores the
  worker's prior context even on failure. It does not transfer credentials or
  servlet objects. Custom executors must explicitly use that decorator.
- Gateway uses Reactor context and a short MDC scope for its completion log;
  it never leaves MDC set across an asynchronous publisher lifetime.
- Scheduled jobs have no HTTP request ID or human actor. Their audit actor is
  SYSTEM. There is no Kafka propagation to install in the current application.
  If messaging is introduced, put request ID in a message header, keep event ID
  separate, propagate tracing through its tracing library, and restore/clean
  consumer context explicitly.
- Discovery-server is infrastructure; no business audit storage is added there.

## Audit records and identity

AuditLog is independent of mutable BaseEntity and has no domain relationships.
It records UTC occurredAt, requestId, traceId, actor ID/type, standardized action,
resource type/ID, before/after JSONB, transport IP, bounded user agent, service,
result and an optional error code. Description is the standardized action name.
Hibernate treats rows as immutable; the upgrade adds an update/delete rejection
trigger. No audit mutation endpoint exists.

AuditContextProvider reads the existing authenticated principal or signed userId
claim. It never uses actor IDs from frontend bodies or identity headers. Service
principals use API_CLIENT. Unauthenticated HTTP actions use ANONYMOUS rather than
SYSTEM. Login uses the successfully verified Authentication explicitly, before a
normal request security context exists. Failed login does not treat a submitted
username as verified identity.

Forwarded headers are disabled in servlet services by default; the recorded IP
is the transport peer. Behind the gateway that is normally the gateway address.
Gateway access logs retain the original transport address and can be joined by
request ID. Do not enable arbitrary X-Forwarded-For trust to obtain a client IP.
For a deployment requiring forwarded client addresses, configure an explicit
trusted-proxy boundary and block direct external access to service ports.

## Transactions and availability

- AuditLogService.record requires an existing business transaction (MANDATORY).
  Business changes and audit rows commit or roll back together. There is no
  after-commit process-crash gap and no uncommitted success visible to readers.
- Audit storage failure fails the business mutation; it is not silently ignored.
- recordSecurity uses REQUIRES_NEW for denied access and authentication failures,
  so a failed outer transaction cannot erase them. The legacy filter login also
  uses this method because it is outside a business transaction.
- Failure-audit storage failure fails closed. Configure connection-pool capacity
  for an outer transaction plus an independent failure-audit connection.
- Promotion lifecycle work uses locked batches of at most 200 records per tick.
  Each transition is recorded individually without loading SKU collections.
- Existing inventory movement records are preserved; the audit row links the
  business change to request and authenticated actor metadata.

## Covered operations

- Promotion create/update/activate/schedule/disable, SKU assignment/removal,
  scheduled activation/expiry.
- Product create/update/disable, SKU create/update, category create/update.
- Inventory initialization, restock, adjustment, stock increase/reduction.
- Login success/failure (service and legacy filter), verification-based login,
  logout with a resolved refresh token, password reset/change, role grants during
  admin user creation, and service access denials.
- Reads are not audited. Action enum values for future operations are vocabulary,
  not an assertion that every module operation is instrumented.

## Safe snapshots and logs

CatalogAuditSnapshots selects scalar fields explicitly. It never serializes
entities, relationships, request bodies or authentication objects. Free-text
descriptions, image payloads and personal user fields are excluded.

AuditSnapshots additionally allowlists field names, rejects nested object graphs,
bounds field counts/string lengths, strips control characters and redacts unknown
fields. Tokens, passwords, hashes, OTPs, secrets and payment credentials are not
snapshot fields. Known raw-password and bearer-token logging was removed; token
validation errors no longer echo exception payloads. Generic server errors no
longer return internal exception messages.

Only put approved business values in snapshots. Never put user secrets in a
business name or metadata field; the allowlist is not a general-purpose content
classifier. Do not enable HTTP body/Authorization logging in production.

## Search API

Directly on each domain service:

    GET /api/admin/audit-logs?requestId=req_support-42&page=1&size=20

Through the gateway, choose the owning service:

    GET /api/admin/audit-logs/catalog?requestId=req_support-42
    GET /api/admin/audit-logs/auth?requestId=req_support-42
    GET /api/admin/audit-logs/order?requestId=req_support-42
    GET /api/admin/audit-logs/payment?requestId=req_support-42

Requires verified ADMIN or AUDIT_READ authority, enforced at both URL and method
levels. Query filters: requestId, traceId, actorId, actorType, action, resourceType,
resourceId, result, serviceName, from, to. Times use ISO-8601 instants, e.g.
2026-10-01T00:00:00Z. Date bounds are inclusive. Page is one-based; size is 1..100.
Sort supports occurredAt or id, direction ASC/DESC; id is the stable tie-breaker.
The standard APIResponse contains the Page under data. Page metadata itself
uses Spring's zero-based page number, matching the project's existing pattern.
Snapshot fields render as JSON objects. No global merged pagination is provided.

Gateway CORS exposes X-Request-ID. Services and gateway include the ID in standard
error bodies and response headers. Request ID is a support correlation value,
not proof of authorization, uniqueness or authenticated origin.

## Deployment and migrations

Each domain service has:

    src/main/resources/db/upgrade/20261002_audit_logs.sql

This follows the existing manually applied db/upgrade convention; it does not
install a second migration runner. Apply the script to each corresponding service
database before deploying. It creates the table, JSONB columns, indexes and
append-only trigger, and can be reapplied.

Use a separate migration owner. The runtime role needs SELECT/INSERT on audit_logs
and USAGE on audit_logs_id_seq; it must not own the table, alter triggers, truncate
or delete records. The trigger blocks UPDATE/DELETE; privileges must also prevent
TRUNCATE and DDL. Use ddl-auto=validate in production after applying migrations;
development ddl-auto=update alone does not install the trigger.

Indexes cover request ID, trace ID, actor/time, resource type/ID and time/ID.
Action-only and JSON indexes are deliberately omitted until measured query load
justifies their write cost. Establish a documented retention period and privileged
archive/purge procedure; ordinary application users cannot purge audit history.
Back up each service database and restrict access to audit search.

## Verification

Run the full reactor build:

    mvn clean verify

Tests cover valid/missing/malformed/duplicate IDs, response headers, exception
cleanup, Feign multi-hop continuity, RestClient, async context restoration,
gateway local errors and header precedence, verified actor and transport IP,
redaction, before/after JSON, combined search filters and pagination, mandatory
success transactions, failure-audit survival after rollback, audit endpoint
authorization, and promotion operation audit trails.

The default database tests use H2 in PostgreSQL mode. Existing optional PostgreSQL
promotion tests require a disposable database and ESHOP_TEST_POSTGRES_URL plus
ESHOP_TEST_POSTGRES_PASSWORD. They must not target an application database.
PostgreSQL-specific upgrade/trigger verification is a separate deployment check.

### Local verification limitation

Docker became unavailable before the PostgreSQL migration could be executed.
An empty temporary database named audit_verification_13db6d80 was created in the
existing eshop-promotion-check-64cc1f16 test container. No application database
was changed. When that container is available again, remove only this temporary
database with:

    docker exec eshop-promotion-check-64cc1f16 dropdb -U postgres audit_verification_13db6d80

The JSONB migration and append-only trigger still need execution against PostgreSQL
before deployment. H2 tests validate persistence and transaction behavior but do
not validate the PostgreSQL trigger.

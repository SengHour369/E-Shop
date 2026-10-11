# Order and payment automation

Implementation date: 2026-10-11. Backend source: `D:\spring boot\E-Shop`.

## Repository analysis and reuse

| Existing implementation | Change |
| --- | --- |
| Order Service `CheckoutCoordinator` and recovery worker | Keep durable checkout, priced catalog snapshots and checkout keys. Online orders reserve stock and wait for verified payment. Cash on delivery keeps existing inventory behavior. |
| Catalog Service `CatalogCheckoutService` | Reuse reserve/confirm/release, keyed by order ID. Add authenticated reservation snapshot reads. No second inventory implementation. |
| Payment Service `BakongServiceImpl` and NBC SDK | Generate a dynamic USD KHQR for the authoritative order amount. Verify stored QR MD5 server-to-server using Bakong. Reuse transaction history and payment records. |
| Legacy `PaymentServiceImpl.processPayment` | Remove simulated successful payment. Reject manual success and manual changes to provider-managed payment records. |
| AI NotificationOutbox, notification events and delivery worker | Keep the existing AI outbox. Extend the same versioned notification contract for commerce lifecycle events, using local commerce outboxes in Order and Payment databases. |
| `AiRouterService`, `AiExecutionService`, `AiToolRegistry`, `AiToolExecutor` | Register `ADMIN_AUTOMATION_REVIEW` as an approved read-only tool requiring `ORDER_VIEW_ALL`. No arbitrary API execution. |
| Existing JWT, audit and request/trace components | Reuse signed service identities, authenticated customer ownership checks, admin method authorization and transaction-bound audit recording. |

The repository contains Bakong/KHQR integration, **not an ABA PayWay hosted checkout adapter**. Selecting ABA uses the existing KHQR path for a compatible banking app. It does not claim a PayWay integration.

## Processing flow

1. Authenticated customer submits the existing from-cart request with a stable checkout key. With automation enabled, Order Service checks the address belongs to that customer through Auth Service.
2. Order Service records `CHECKOUT_PENDING`, then reserves inventory through the existing Catalog checkout API. Catalog supplies authoritative prices and promotions. Cart values cannot set the order price.
3. The same Order database transaction stores priced order items, changes the order to `PENDING`, clears the cart, and inserts `ORDER_CREATED` plus lifecycle notification outbox records.
4. The outbox worker publishes to Kafka only after database commit. It marks publication complete after broker acknowledgement. A crash between publication and acknowledgement can redeliver; consumers deduplicate by event ID and direction.
5. Payment Service persists an inbox record before the Kafka consumer returns. Its worker re-reads the authoritative order and creates one order-bound KHQR transaction using the existing SDK. A partial unique index allows only one issued QR per order.
6. Scheduled verification polls Bakong for the **stored** QR MD5. The customer can also request a provider check. Both paths use the same verifier. The browser cannot submit a payment status or proof.
7. Successful verification requires response code `0`, no provider error, a valid provider transaction hash, and exact recipient, currency and amount matches. A success word, chat message, screenshot, QR scan or HTTP 200 alone is insufficient.
8. Payment status, status history, provider evidence, audit entry and `PAYMENT_VERIFIED` outbox event commit together in the Payment database.
9. Order Service consumes the event, then re-reads the authoritative Payment Service evidence. For a pending order it confirms the existing reservation and moves the order to `PROCESSING`, recording payment transaction ID, audit and notification outbox records together.
10. If the Catalog confirmation succeeds but the Order transaction fails, the inbox retry repeats the same idempotent Catalog operation and completes the local transaction. There is no distributed database transaction.
11. A QR that has expired plus a 60-second grace period is failed only after a trusted provider `not found` response. Order Service rechecks the negative state, releases inventory, cancels the pending order, and queues failure/cancellation notifications.
12. Verification outages, malformed evidence and amount/recipient mismatches remain unresolved and create review alerts. They never prove success or justify releasing stock. Failed/expired QR transactions remain eligible for hourly late-transfer checks for 30 days.
13. A verified late transfer on a cancelled order creates a review alert and payment notification. It never revives the order. Human-approved refund handling is required.
14. Notification Service uses its existing idempotent event receiver, durable in-app records and retrying email delivery worker. Email uses the existing opt-in preference, currently named `aiEmail`; it is not a new channel preference system.
15. Authorized admins can inspect the combined order, payment, reservation and notification state. Explicit review requests persist inconsistency alerts. The approved AI inspection tool reads this same data without changing state. Core processing does not depend on Ollama or any AI provider.

## API examples

All gateway requests require the existing bearer JWT. Supply `X-Request-ID`; W3C trace context is propagated when present. Numbers and identifiers below are examples, not real payments.

### Create an order

`POST /api/v1/orders/user/from-cart?userId=42`

```json
{
  "address_id": 7,
  "payment_method": "BAKONG",
  "currency": "USD",
  "checkout_key": "checkout-42-unique-attempt"
}
```

The existing order response envelope is preserved. Its order payload contains `id`, `order_number`, `status`, `total_amount` and priced items. Successful online preparation returns `PENDING`; recoverable preparation can remain `CHECKOUT_PENDING`. Neither means paid. Reuse the checkout key after an uncertain response.

### Read the order QR

`GET /api/v1/payments/orders/101/qr`

```json
{
  "paymentId": 55,
  "orderId": 101,
  "amount": 10.00,
  "currency": "USD",
  "status": "PENDING",
  "qr": "<SDK-generated dynamic KHQR payload>",
  "expiresAt": "2026-10-11T06:15:00Z",
  "provider": "BAKONG_KHQR"
}
```

`GET /api/v1/payments/orders/101/qr/image` returns an authenticated PNG for a pending, unexpired QR. Customer ownership is checked against Order Service. A QR that is not ready returns a business error; no example or fallback payment code is shown.

`POST /api/v1/payments/orders/101/verify` with `{}` requests server-side verification and returns the current QR metadata. It can remain `PENDING`. Only provider evidence can produce `SUCCESS`.

The commerce event's `paymentId` and the order's new automated payment reference identify `payment_transactions.id`, not the separate legacy `payments.id`. Preserve this distinction when displaying legacy records.

### Verified event contract

Topic: `commerce.payments.v1`; partition key: order ID.

```json
{
  "version": 1,
  "eventId": "3b70da93-8acd-457d-aa40-63f6d22a7fd2",
  "type": "PAYMENT_VERIFIED",
  "orderId": 101,
  "userId": 42,
  "amount": 10.00,
  "currency": "USD",
  "paymentMethod": "BAKONG",
  "paymentId": 55,
  "requestId": "req-example",
  "traceId": "0123456789abcdef0123456789abcdef",
  "occurredAt": "2026-10-11T06:02:00Z"
}
```

Order events use `commerce.orders.v1`. Lifecycle notifications use the existing notification topic and version 1 contract. Customer/admin recipients are derived from the authoritative order and configured admin IDs, never from AI output.

### Admin inspection

| Method and path | Purpose |
| --- | --- |
| `GET /api/v1/orders/automation/events?page=0&state=DEAD&orderId=101` | Order inbox/outbox executions; up to 50 per page |
| `GET /api/v1/payments/automation/events?page=0` | Payment inbox/outbox executions |
| `GET /api/v1/{orders or payments}/automation/events/{eventId}/logs` | Latest 100 attempts |
| `POST /api/v1/{orders or payments}/automation/events/{eventId}/retry?direction=INBOX` | Retry a retained dead event; records the requesting admin |
| `GET /api/v1/{orders or payments}/automation/reviews` | Latest 100 review alerts |
| `POST /api/v1/orders/automation/orders/101/review` | Read authoritative services and persist detected review alerts |
| `GET /api/notifications/admin/delivery?orderId=101` | Latest 100 delivery records, including email attempts and status |

Example execution row:

```json
{
  "event_id": "3b70da93-8acd-457d-aa40-63f6d22a7fd2",
  "direction": "INBOX",
  "order_id": 101,
  "event_type": "PAYMENT_VERIFIED",
  "state": "DONE",
  "attempts": 0,
  "request_id": "req-example",
  "last_error": null
}
```

`attempts` counts failures. Successful processing is recorded in execution logs. Retry returns `{"eventId":"...","state":"PENDING"}`; retrying a non-dead row returns HTTP 409. Review results include `findings` and `financialChangesAllowed:false`.

Internal fixed clients use `SERVICE_ORDER` or `SERVICE_PAYMENT` authorities. Internal routes are not added to the public gateway. Admin monitoring endpoints require `ADMIN`; the AI review also retains the tool registry's permission checks.

## Retry and reconciliation rules

- Each inbox/outbox row is unique by `(event_id,direction)`. Workers use PostgreSQL row locks and `SKIP LOCKED` to share work across instances.
- Business work and inbox completion use one local transaction. Failed work rolls back before a separate transaction records failure and retry time.
- Exponential retry delay starts at 10 seconds and caps at 300 seconds. The tenth failure marks the row `DEAD`, retains it, and creates a review alert.
- Dead events publish to `<original-topic>.DLT`; failed DLT publication remains durable and retries. Invalid Kafka payloads use the Kafka error handler and DLT.
- Kafka publication is at least once. Stable notification IDs and idempotent Catalog operations protect duplicate delivery.
- QR verification runs every 15 seconds by default. Provider errors defer verification and create an alert after repeated failures. A provider outage can retain a stock reservation; admins must resolve this from authoritative evidence.
- Late transfers, mismatches and exhausted workflows require review. The implementation does **not** automatically refund a transfer, invent a successful payment, or use AI to choose financial compensation.

## Frontend

- `/admin/automation`: Order/Payment execution monitoring, state/order filters, execution logs, explicit dead-event retry, review alerts and notification delivery.
- `src/app/features/admin/automation/`: monitoring API contracts, page, responsive stylesheet and API tests.
- `src/app/features/payments/payment-qr.ts`: shared customer QR component used by order details and chat checkout. Authenticated blob requests avoid exposing QR URLs without JWT. Blob URLs are revoked on replacement/destruction.
- Chat checkout retains explicit order review/confirmation. The QR check sends no claimed status, amount or provider hash from the browser.
- Payment status and order processing status are separate. A verified payment can precede order/inventory completion; refresh order details after verification.

## Configuration and rollout

Automation defaults to disabled. The supplied local Bakong settings are saved in the backend's ignored `.env`, with automation disabled. The supplied provider URL is production, and the `random-subdomain.trycloudflare.com` URLs are placeholders. No real transfer or live merchant verification has been performed.

Required payment configuration:

```dotenv
BAKONG_ACCOUNT_ID=<registered merchant account>
BAKONG_EMAIL=<registered Bakong email>
BAKONG_BASE_URL=<official provider endpoint for your environment>
KHQR_MERCHANT_NAME=<registered merchant name>
KHQR_MERCHANT_ID=<registered merchant identifier>
KHQR_ACQUIRING_BANK=<registered acquiring bank>
AUTOMATION_ADMIN_USER_IDS=<comma-separated existing admin user IDs>
```

The existing token service renews/caches a token using the configured email. `BAKONG_AUTH_TOKEN=your_auth_token_here` is not a usable credential and is not consumed by that service. Token errors leave payments unresolved.

Apply these scripts using the existing manual database upgrade convention, after base schemas exist and before enabling workers:

1. Order DB: `order-service/src/main/resources/db/upgrade/20261011_commerce_automation.sql`.
2. Payment DB: `payment-service/src/main/resources/db/upgrade/20261011_commerce_automation.sql`.
3. Notification DB: `notification-service/src/main/resources/db/upgrade/20261011_commerce_notifications.sql`.
4. AI DB: `ai-service/src/main/resources/db/upgrade/20261011_automation_review_intent.sql`.

Existing project databases were previously deleted at the owner's request except auth. This implementation does not recreate them or apply upgrades to the user's databases. Restore/provision the required base schemas before deployment.

`compose.automation.yaml` is an opt-in overlay on the existing Compose files. It enables workers and configures Kafka string serialization, broker acknowledgements and producer idempotence. It requires explicit Bakong account/email and the payment service fails startup without merchant details. The local `.env` alone is not loaded by a directly launched Spring process; export variables or pass the appropriate environment file through your launcher. Do not add the automation overlay until migrations and merchant configuration are ready.

Before production deployment, configure Kafka authentication/ACLs, TLS and replication appropriate to your cluster. The topic declaration defaults to three partitions and one replica for the existing local single-broker environment. Verify provider connectivity, actual merchant settlement, duplicate/reordered callbacks or polling responses, live Kafka failover, customer delivery channels and recovery procedures in staging.

## Verification and remaining operational checks

Automated coverage includes provider evidence validation, amount/currency/recipient mismatches, refusal of success words, unavailable providers, expiry/grace behavior, duplicate payment delivery, late payments on cancelled orders, inventory outages, stock release idempotency, valid order transitions and customer QR errors/expiry.

The optional `AutomationPostgresTest` uses an isolated PostgreSQL database selected through `AUTOMATION_TEST_JDBC_URL`. It verifies repeatable upgrades for Order, Payment, Notification and AI schemas, one issued QR per order, outbox rollback, inbox deduplication, handler rollback/retry, retained dead events and broker failure. It must never target a project or production database: the test owns and recreates its `automation_test` schema.

The real NBC SDK/PNG test exercises QR generation without a provider network call or transfer. Local automated checks cannot establish that an actual merchant account can settle a payment. Live provider verification, a full deployed Kafka/service workflow and customer email delivery still require configured staging services and merchant access. AI inspection is available on demand; automatic generated AI narrative summaries are not a payment-processing dependency.

Provider references: [NBC Bakong Open API](https://bakong.nbc.gov.kh/download/KHQR/integration/Bakong%20Open%20API%20Document.pdf), [ABA PayWay checkout documentation](https://developer.payway.com.kh/folder-3158159).

Final local checks: Angular production build passed; 124 frontend tests passed; affected Java services tested and packaged; 5 PostgreSQL migration/transaction tests passed; real NBC SDK/PNG rendering passed; 7 checkout integration tests and 6 payment-routing tests passed. These checks include mocks for remote service/provider calls and do not establish live merchant settlement.

### Authorized production connectivity check

On 2026-10-11, the owner explicitly authorized token renewal and a nonexistent-transaction lookup against the configured production Bakong endpoint. Token renewal returned `responseCode: 0` and a token. The random MD5 lookup returned `responseCode: 1`, `errorCode: 1` (not found), matching the verifier's negative lookup policy. No payment was initiated, no token was printed or persisted, and no real settlement was tested. This confirms provider authentication/connectivity, not completion of the deployed order, inventory and notification workflow. The local payment database was absent during inspection and application services were stopped.


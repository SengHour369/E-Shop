# AI Automation and live verification — observed gaps

Scope of this slice: a real HTTP testing module, terminal client, and Admin Live Tests UI. The supplied v3 specification is the acceptance reference; this slice does not claim to implement the missing workflow platform.

| Capability / screens | Observed implementation | Reuse / modify | Storage / events | Coverage and next action |
|---|---|---|---|---|
| Identity and tool authorization | auth-service AccountAccessService; common LivePermissionService; ai-service registry/AiToolRegistry.java | Reuse current JWT, groups and live permission checks; add SYSTEM_TEST_VIEW/RUN definitions only | Existing function_permissions/api_permissions, no group grants added | Real anonymous rejection + effective-access checks; add customer/manager test accounts to verify each role separately |
| Live environment testing (new) | Existing ad hoc scripts and smoke-api.cjs | Extend admin-service with fixed, sequential HTTP checks; terminal client calls same runner | live_test_runs and existing audit_logs; no new Kafka topic | Real gateway/service requests and saved metadata-only reports |
| AUT-01–06 workflow list and wizard | No workflow controller/domain package found under ai-service/src/main/java; current Angular automation pages expose existing service automation events | Do not invent production workflow records or counts; add an availability check | Existing automation events/inbox/outbox belong to commerce state machines | Full five-step authoring, immutable versions and graph validation remain missing; live test reports absence/failure |
| AUT-07–09 details and builder | AI tool registry/executor exist; no generic workflow graph persistence found | Reuse registry and deterministic handlers for a later workflow implementation | ai-service/src/main/resources/db/upgrade contains AI orchestration/import/training migrations, not generic workflow graph tables | Builder and graph routing cannot be marked verified by HTTP availability checks |
| AUT-10–11 executions/detail | Existing commerce automation execution/events and AI conversation/execution controllers | Reuse existing audit and request IDs; do not substitute test reports for business workflow executions | Existing commerce.orders.v1/commerce.payments.v1 and audit logs | Generic workflow version/step execution history remains a separate implementation phase |
| Payments and stock | Existing order-service CheckoutCoordinator, payment-service automation, catalog reservations | Fixed live checks read real lists, stock and supported payment methods | Existing reservations and commerce events | No bank payment or stock mutation performed. KHQR receiving-account configuration is missing; availability check must be BLOCKED |
| Notifications | Existing notification-service/Kafka and auth mail handlers | Read current account unread count only | Existing notification topics/outbox | Does not verify delivery or send test email; recipient/channel authorization must precede a delivery test |
| AI / English / Khmer | ai-service AiController, AiToolRegistry, AiToolExecutor; ai-inference provider layer | Check real provider information and conversation API, without invoking mutating AI proposals | Existing AI execution/conversation storage | Provider-info availability is not inference accuracy. English/Khmer generation, injection and provider-failure acceptance require a separate controlled inference suite |
| Public / guest access | StorefrontAccessClient and order-service guest session/cart/checkout handlers | Read saved flags; optional isolated real guest cookie bootstrap/revoke | Existing guest_sessions; no cart/order/payment created by test module | Real network calls; deployment using secure cookies needs an HTTPS gateway URL for guest session probe |

## Practical screen-aligned follow-up

1. Deliver and verify Live Tests with real statuses, progressive steps, saved report, ownership, permission checks and terminal export.
2. AUT-01–06: implement generic workflow draft/version/graph APIs before authoring UI; never return screenshot samples as data.
3. AUT-07–09: add immutable published-version editing, registered tools and validated graph bindings.
4. AUT-10–11: add real version-pinned execution/step history, approval and reconciliation, then validate those with real controlled scenarios.
5. Extend the test catalog only with reviewed fixed handlers. Payment settlement, refunds, publishing, inventory mutation and notification delivery are deliberately absent from this runner.

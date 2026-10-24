# Admin AI service access

Admin AI reads database-backed records through fixed service APIs. It does not connect the model to databases, accept arbitrary URLs or run SQL. Existing account permissions and resource ownership apply on every request; being in an admin conversation never grants an absent permission.

Admin voice sessions now export **every enabled tool available to the current administrator in AiToolRegistry**. Previously voice maintained a shorter manual subset. Existing voice aliases continue to work; additional tools receive stable `admin_…` aliases. New registry entries become available without another voice-specific mapping. Customer voice retains its existing customer-only tool list.

Supported integrations span:

- Catalog: products, SKU attributes, prices, stock, low stock, promotions and existing draft/import workflows.
- Orders: owned cart/checkout/orders, authorized admin order and return lists, order summaries and automation review.
- Payments: owned payment status/KHQR and authorized admin payment/revenue reads.
- Authentication: authorized account summaries, without credentials or contact details.
- Notifications: current account notifications and automation delivery summaries; private message bodies remain excluded from model data.
- Admin service: new read-only monitor-status and storefront guest-access feature-flag endpoints.
- AI service: audit lookup, knowledge, existing approval/draft flows and new capability discovery.

`ADMIN_TOOL_LIST` is a read-only, administrator-only registered tool. Ask “Which service APIs can you access?” to get the enabled operations for the current grants, including service, fixed endpoint, parameters and confirmation requirement. Voice uses `admin_tool_list`. `ADMIN_SERVICE_STATUS` uses `admin_service_status`; `ADMIN_STOREFRONT_ACCESS` uses `admin_storefront_access` and requires `STOREFRONT_ACCESS_VIEW`.

Write behavior is unchanged: the existing dispatcher stages enabled changes for explicit UI confirmation; bulk imports retain independent approval requirements. Disabled refund/role-grant tools remain disabled. Provider output cannot enable them. AI cannot confirm its own action. Query sizes, ownership constraints and service pagination limits still apply.

This is access to all **registered, enabled, permitted AI operations**, not every HTTP endpoint or every database table. Unregistered APIs require a fixed client method, typed schema, permission rule and tests before the AI can use them. New permissions must be assigned through the existing access-management UI.

Implementation files: `VoiceTools`, `LiveVoiceController`, `AiToolRegistry`, `AiToolExecutor`, `AiChatPresenter`, `AiIntent`, and the new `AdminToolClient`. Tests include `AdminAiServiceAccessTest`, existing registry/confirmation tests and workflow execution tests. No credentials, new account grants or database schema changes are included.

## Validation

The AI-service and common-library Maven suites completed with zero failures/errors: 209 AI-service tests (185 passed, 24 skipped) and 29 common-library tests (24 passed, 5 skipped). Focused follow-up tests also passed after checking that model-visible capability parameters retain their names/types and that SQL-like aliases are rejected. These are automated API/client tests, not a claim that every remote endpoint was manually exercised or that a financial change was executed.

Restart an existing voice call to load the expanded ephemeral-token tool declarations. Text chat uses the current registry immediately after the AI-service deployment.

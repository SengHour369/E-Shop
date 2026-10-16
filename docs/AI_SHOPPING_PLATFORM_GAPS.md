# Shopping assistant: implementation and deployment gaps

The initial repository inventory is in [AI_ASSISTANT_REPOSITORY_ANALYSIS.md](AI_ASSISTANT_REPOSITORY_ANALYSIS.md). [AI_PRODUCT_SEARCH.md](AI_PRODUCT_SEARCH.md) describes the first product-search increment. This report records the subsequent shopping and admin increment; it distinguishes code verified by tests from configured live integrations.

## Implemented and reused

| Capability | Implementation and authority |
|---|---|
| Search and details | Registered bilingual PRODUCT_SEARCH plus PRODUCT_DETAIL / PRODUCT_VARIANTS / PRODUCT_STOCK_CHECK. Catalog supplies actual products, gallery, SKU attributes, effective promotion prices and availability. Angular renders inline details and variant selection. |
| Cart | CART_GET / ADD / UPDATE / REMOVE dispatch to fixed owned Order Service adapters. Explicit tool confirmation precedes writes. Cart receipts bind actor, operation and arguments to an idempotency key. Existing UI controls also send mutation keys. The whole basket is quoted together to preserve basket promotion semantics. |
| Checkout | Address selection and address creation remain inside chat. Auth verifies address ownership and account state. CHECKOUT_PREVIEW refreshes the cart and stores a short-lived owner-scoped review. ORDER_CREATE consumes the reviewed identifiers, compares the cart under lock, rechecks inventory and price, and reuses CheckoutCoordinator's durable checkout key. The ordinary in-chat checkout also sends reviewed line quantities and total. |
| Payment | PAYMENT_INITIATE_KHQR and PAYMENT_GET_STATUS use the existing Payment Service, whose order event automation creates provider payment attempts. Chat renders its QR/status through the existing PaymentQr component. Available methods are derived from configured merchant/provider settings. Only trusted backend verification can mark payment successful. |
| Orders | Owned order list/detail and eligible cancellation reuse existing domain APIs. Chat displays real current state and available timestamps, line items and payment controls. Cancellation requires explicit confirmation. No historical shipping events are fabricated. |
| Returns and notifications | RETURN_GET_STATUS and NOTIFICATION_LIST read owned service data. RETURN_CREATE stays disabled pending a concrete return policy and safe domain eligibility/amount handling. |
| Visual recognition | Authenticated photo upload or camera capture uses PRODUCT_VISUAL_SEARCH. Inference identifies labels; Catalog fetches real candidates. CatalogVisualComparison compares the original image against actual catalog images through authorized Gemini vision when configured. Downloads are bounded and restricted to the configured Cloudinary cloud; model output can select only supplied candidate indices. Customers must select a candidate. If comparison is unavailable, the response explicitly reports label candidates and unverified visual similarity. No barcode/QR scanning is used. |
| Context | Authenticated Redis context is owner-scoped and bounded, with a 30-minute TTL. Guests retain no server-side conversation state. The browser stores only its owned conversation ID, and resets expired IDs. Existing authenticated admin transcripts are reused. This is not permanent customer transcript storage. |
| Admin operations | Existing Excel staging, mapping, validation, independent review, approval, execution, signed proof and atomic receipts are reused. Existing creation, inventory and promotion tools remain. CATEGORY_BULK_UPDATE, BRANCH_BULK_UPDATE and PRODUCT_BULK_UPDATE now require actual record IDs and reviewed versions. ADMIN_IMPORT_DRAFT validates one registered import schema and stages a mapped one-row workbook after explicit confirmation; it does not execute catalog writes. Product images must be attached during review. Detailed product requests use this chat workflow; existing manual admin forms remain available. |
| Reliability and security | Fixed registry/Feign dispatch, canonical validation, live permissions, JWT identity and ownership checks, request fingerprints, request IDs, audit records, existing Kafka events/outbox, bounded timeouts and existing checkout/payment recovery are reused. Structured UI actions bypass inference but use the same authorization and execution boundary. Cached responses are reauthorized. |

## Main changed files and dependencies

All paths below are relative to the indicated repository root.

### E-Shop backend

- `ai-service/.../registry/AiToolRegistry.java`, `AiDraftArguments.java`; `.../service/AiRouterService.java`, `AiToolExecutor.java`, `AiExecutionService.java`, `AiImportService.java`, `AiChatStateService.java`, `AiChatPresenter.java`, `AiImageAttachment.java`; `.../controller/AiController.java`: fixed tools, confirmation, safe argument staging, owner context, visual upload and response cards.
- `ai-inference/app/services/product_search.py`, `intent_service.py`, `prompt_service.py`; `app/providers/gemini.py`, `app/api/vision.py`, `app/api/import_mapping.py`: bilingual extraction, allowlisted tools and authorized visual comparison. Khmer numerals are normalized; all product data still comes from Catalog.
- `catalog-service/.../service/impl/AiProductSearchService.java`, `.../controller/AiCatalogController.java`, `ProductScannerController.java`, `.../scanner/CatalogVisualComparison.java`: real search/projections and bounded candidate ranking.
- `common-lib/.../imports/ImportTool.java`, `catalog-service/.../service/CatalogImportService.java`, `.../service/BranchService.java`, Product/Category entities and response mappers: approved update schemas, permissions, optimistic versions and receipt-backed execution.
- `order-service/.../service/impl/AiShoppingService.java`, `CartServiceImpl.java`, `CheckoutCoordinator.java`, `OrderServiceImpl.java`; `.../controller/AiShoppingController.java`, `CartController.java`, `AiReturnController.java`; `.../model/AiMutationReceipt.java`: owned adapters, fresh quotes, durable writes and order boundaries.
- `auth-service/.../service/impl/AddressServiceImpl.java`, `.../repository/AddressRepository.java`; `order-service/.../automation/CheckoutAddressVerifier.java`: address ownership and live account checks.
- `payment-service/.../controller/PaymentMethodsController.java`, `.../automation/PaymentAutomation.java`: configured payment availability and customer QR ownership.
- `compose.ai.yaml`: shared private vision service key wiring. No key is generated or populated by this change.

### Order_Shop_Frontend

- `src/app/features/assistant/customer-chat/`: conversation cards, inline details, camera/upload, tool confirmations, context and idempotent cart retries.
- `src/app/features/assistant/chat-checkout/`: owned addresses, fresh checkout review and supported payment methods.
- `src/app/features/assistant/chat-order.ts`: owned order detail and cancellation confirmation.
- Existing assistant API/cart/checkout models and `src/app/features/admin/assistant/`, `src/app/features/admin/imports/`: registered actions, import draft handoff and update labels.

## Database and configuration prerequisites

Apply the reviewed migrations to the corresponding service databases before deployment. At the initial shopping increment they were not applied. During the subsequent local Bakong activation, migration 2 below and the existing Order/Payment commerce-automation migrations were applied to the local Docker databases; the other migrations below remain unapplied. See `BAKONG_VERIFICATION.md`.

1. `db/migrations/20261016_ai_execution_fingerprint.sql` — AI execution argument fingerprint.
2. `db/migrations/20261016_ai_shopping.sql` — Order mutation receipts and expected checkout total.
3. `db/migrations/20261016_ai_shopping_intents.sql` — AI execution intent constraint, including ADMIN_IMPORT_DRAFT.
4. `db/migrations/20261017_catalog_approved_updates.sql` — Catalog product/category optimistic version columns.
5. `db/migrations/20261017_auth_approved_updates.sql` — CATEGORY_UPDATE and BRANCH_UPDATE function definitions, **without granting them to users**. Assign grants intentionally through existing administration.

Use the existing private import signing key consistently between AI and Catalog. Set the same private `AI_VISION_SERVICE_KEY` for Catalog and AI inference, and configure the real Cloudinary cloud and Gemini provider. Visual fallback remains explicit when comparison cannot run. Real KHQR requires the configured recipient, a registered Bakong verification email and payment reconciliation configuration. INDIVIDUAL mode does not require merchant ID/acquiring bank; MERCHANT mode retains registered merchant details. Keep all credentials out of source control.

## Unresolved business and deployment items

- Return/refund submission needs a defined eligibility window, delivered-at source, refundable line quantities/amount calculations and review rules. The chatbot deliberately exposes status only. The legacy return-create API still needs this domain hardening before it can be reused safely.
- Existing checkout does not provide a general tax/shipping calculator. This implementation recalculates existing authoritative price/promotion/inventory data and rejects changed totals; it does not invent tax or shipping charges.
- The repository does not provide a complete immutable order/shipping event timeline. Chat shows current authoritative state and real stored timestamps; richer tracking requires persisted domain events.
- Visual ranking compares a bounded candidate shortlist obtained from labels; it is not a catalog-wide embedding index and does not guarantee exact identification.
- A separate ABA PayWay acquiring integration is not introduced. ABA is supported through the existing KHQR flow where configured.
- Customer context expires; permanent conversation history requires a separate retention and deletion policy.
- Builds and isolated tests do not certify live SMTP, Cloudinary uploads, real bank settlement or configured production credentials. No payment, order, migration or deployment was performed against production as part of verification.

See [AI_SHOPPING_VALIDATION.md](AI_SHOPPING_VALIDATION.md) for the final test run and its limits.

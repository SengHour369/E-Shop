# Voice product discovery extension — 2026-10-23

The existing customer and admin chat now receive catalog cards and voice transcripts from the same Live session. The original chat components, confirmation controls, checkout and payment renderers remain in use. Call limits remain five per Cambodian business day, 300 seconds per call, one active credential per user, free-project attestation required, and no paid fallback.

## Behavior and integrations

- `LiveVoice` exports schema-validated, versioned events. Tool operations run in order. Session-scoped call IDs and canonical arguments suppress retries within a turn; event IDs and product-detail fingerprints suppress duplicate cards. Responses from another conversation are rejected.
- A compact sticky call bar displays status, remaining time, mute, call details and End. Returning from call details minimizes the panel while preserving audio. Product images, names, detail buttons and SKU selectors remain usable.
- `POST /api/ai/voice/sessions/{id}/selection` validates session ownership, loads `PRODUCT_DETAIL` through the existing authorized/idempotent dispatcher, verifies any supplied SKU belongs to that product, and refreshes SKU price/stock through `PRODUCT_STOCK_CHECK`. It returns a typed event, existing chat presentation, minimized model data and authoritative context.
- Selection flushes queued playback, suppresses the old response, and queues an ID-only notice until the current model turn completes. The current official SDK's `sendRealtimeInput({text})` delivers the notice over the existing connection. Gemini then fetches current product data before explaining. No new Live session is created. See Google's [Live capabilities](https://ai.google.dev/gemini-api/docs/live-api/capabilities) and [tool response protocol](https://ai.google.dev/gemini-api/docs/live-api/tools).
- Product/SKU selection, verified displayed order, language, session identity and expiry are merged atomically into actor-scoped `AiChatStateService` Redis context. Changing products clears an old SKU; sorting/filtering can only reorder/subset actual search results. Updates preserve unrelated checkout/order references. Expired call context reads as inactive.
- Existing catalog search now searches actual SKU attribute values in addition to names, descriptions and category names. Price filters use effective promotion prices; stock uses inventory availability. Metadata is batched. Details include descriptions, attributes, variant prices, stock and actual promotion dates/discounts.
- Customer cards and admin cards use the existing product mapper/renderers with metadata, inline specifications, variants and promotion information. Admin inventory result rows can select their product/SKU into the call. UI confirmation completion requests a fresh authoritative lookup from Gemini.
- Admin sessions explicitly request `ADMIN` mode. Creation and each tool/selection recheck current administrator status. Tool declarations and execution retain registry permissions. Added admin aliases cover stock, low stock, orders, summaries, payments, revenue, promotions and import draft staging. New promotion list/SKU intents call existing Catalog Admin APIs and require `PROMOTION_VIEW`. Customer mode never exposes admin tools. Admin mutations still use existing confirmation and independent approval workflows.
- Existing cart, owned addresses/orders, checkout review, order staging, KHQR and authoritative payment-status tools are reused. No model-callable confirmation or new checkout/payment service was introduced.

## Data constraints

Catalog has no product-level code column or brand relation. `productCode` is therefore null; real SKU codes remain available. Brand is exposed only when stored Brand/ម៉ាក SKU attributes identify one value; it is never inferred from a name. Category comes from the stored category relation. USD follows the existing AI checkout currency contract. Missing data remains unavailable. Search is capped at 20 products; admin promotion lookups currently return the first 20 promotions and first 100 assigned SKUs, so broad queries may need narrowing.

Only the field allowlist reaches Google. Contact details, full addresses, tokens, payment QR payloads and notification message bodies are excluded. Audio is not persisted. Voice transcripts enter the existing chat messages/history; customer history follows the existing local persistence behavior. Ending/closing/changing conversation releases microphone tracks, playback, timers and client queues; issued credential lease restrictions remain unchanged.

## Verification

- Backend: common-lib, catalog-service and ai-service suites passed. Current reports total **357 tests: 322 passed, 35 skipped**. Focused Redis tests explicitly ran with `VOICE_REDIS_TEST=true` against isolated actor namespaces. Coverage includes concurrent context merges, empty search arrays, display-order validation, selection/SKU ownership, current admin permissions, minimization, free limits and lease accounting. Catalog integration tests cover mixed Khmer/English attribute search, live promotion price, stock and stored metadata.
- Frontend: **122 tests passed** across 20 assistant/admin suites. Coverage includes product-card interaction with compact call controls, same-session SKU selection, queued notices, tool retries, schema rejection, transcripts, audio cleanup, checkout/payment authority and existing renderers.
- Angular production build passed. Existing initial-bundle budget and Google SDK CommonJS warnings remain.
- Deployed AI/catalog containers and frontend assets to the existing HTTPS setup. Authenticated configuration and usage returned HTTP 200 at `https://192.168.18.176:8443`; enabled=true, limit=5, duration=300. This HTTP preflight used an isolated signed test identity and did not consume a call reservation or establish account permissions.
- Real Gemini Developer API tests with customer declarations and permission-filtered admin declarations each used one ephemeral-token Live connection. In both, synthetic mixed Khmer/English **text input** caused `search_products` to retrieve real product 2 (Astronaut Explorer Figure); a UI selection notice triggered `get_product_details` for that same ID on the same connection. Both produced PCM audio and Khmer output transcription (customer 622,140 bytes; admin 487,260 bytes). These tests routed only public catalog reads; admin authorization was separately tested on the backend. First provider attempt coincided with catalog restart and failed to fetch; reruns after readiness passed.
- Real local search found no matching Nike shoes; no product was fabricated to make that example succeed.

Not verified: human microphone end-to-end use, audible Khmer pronunciation quality, selecting a real black size-42 SKU (not present in the searched product), real voice-driven cart/order creation or Bakong settlement, and browser visual acceptance. Automated commerce tests preserve confirmation/payment authority; they do not prove a live payment occurred.

Raw local reports are under ignored `reports/voice-product-*`; they contain no permanent API key or ephemeral token. Provider tests used synthetic text rather than a recording and did not write carts, orders or payments.

## Files changed for this extension

Backend paths relative to `/home/senghour/Desktop/E-Shop`:

- `ai-service/src/main/java/com/example/eshop/ai/voice/{LiveVoiceController,VoiceSessions,VoiceTools,VoiceEvent}.java`
- `ai-service/src/main/java/com/example/eshop/ai/service/{AiChatStateService,AiChatPresenter,AiToolExecutor}.java`
- `ai-service/src/main/java/com/example/eshop/ai/{enums/AiIntent,registry/AiToolRegistry,client/CatalogToolClient}.java`
- `catalog-service/src/main/java/com/example/eshop/catalog/{controller/AiCatalogController,repository/ProductRepository,service/impl/ProductResponseService,service/impl/AiProductSearchService}.java`
- `ai-service/src/test/java/com/example/eshop/ai/AiShoppingContextTest.java`
- `ai-service/src/test/java/com/example/eshop/ai/voice/{LiveVoiceControllerTest,VoiceSecurityTest,VoiceContextRedisTest}.java`
- `catalog-service/src/test/java/com/example/eshop/catalog/service/{AiProductSearchTest,AiProductMetadataTest}.java`

Frontend paths relative to `/home/senghour/Desktop/Order_Shop_Frontend`:

- `src/app/features/assistant/customer-chat/{live-voice,voice-events,voice-call,chat-message,customer-chat}.ts`
- `src/app/features/assistant/customer-chat/customer-chat.html`
- `src/app/features/assistant/customer-chat/{voice-events,live-bridge,customer-chat}.spec.ts`
- `src/app/features/assistant/{assistant.page,assistant-product,assistant-result}.ts`
- `src/app/features/admin/assistant/{admin-assistant.page.ts,admin-assistant.html}`

Earlier HTTPS/transcription/voice setup changes in these dirty repositories were preserved; this list describes this extension rather than every preexisting workspace change.

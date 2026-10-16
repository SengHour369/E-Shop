# Registered chatbot product search

This extends the existing PRODUCT_SEARCH tool, fixed Catalog Feign client, public catalog adapter and Angular customer-chat cards. Checkout behavior and the ShopAdmin layout are unchanged. Product records, image URLs, prices, promotion names and available stock come from Catalog; no sample products are returned by production code.

## Request flow and files

1. `ai-inference/app/services/product_search.py` recognizes conservative English/Khmer search phrases, converts Khmer digits and extracts price/stock filters. Generic category vocabulary is translated; brand/model words are preserved. Complex requests continue through the existing structured provider. `intent_service.py` only selects tools in the caller's catalog; `prompt_service.py` describes the search contract.
2. `ai-service/src/main/java/com/example/eshop/ai/registry/AiToolRegistry.java` validates required `query` (maximum 120 characters), optional numeric `minPrice`/`maxPrice` (USD, 0–1,000,000, at most two fractional digits) and optional boolean `inStock`. Reversed ranges and unknown keys are rejected before any catalog request. Price bounds are inclusive.
3. `AiToolExecutor.java` forwards only these arguments through `client/CatalogToolClient.java` to GET `/internal/ai/products`. No provider-supplied URL or HTTP method is accepted. Existing JWT checks, live permissions, owner-scoped private tools, conversation isolation, request IDs, audits and confirmation/idempotency controls remain in place.
4. `catalog-service/src/main/java/com/example/eshop/catalog/controller/AiCatalogController.java` delegates search to `service/impl/AiProductSearchService.java`. `repository/ProductRepository.java` supports JPA specifications. Query words match product name/description or category names, with AND across words, escaped LIKE wildcards, and active/non-deleted restrictions. Brand matching uses existing catalog text; there is no invented brand relationship.
5. Search loads candidates in batches of 100, uses existing `ProductResponseService` and `PromotionPricingService`, filters actual effective SKU prices and available stock, and returns at most 20 products in ID order. It continues beyond the first candidate page so expensive early matches do not hide later budget matches. Only matching SKUs are included in cards. Price filtering cannot use base price alone because promotions can make a product affordable.
6. `ai-service/.../service/AiChatPresenter.java` supplies English/Khmer no-results and safe service-error messages. Product cards include IDs, names, descriptions, images and SKU base/effective price, availability, default flag, promotion flag/name and discount values.
7. Angular files under `/home/senghour/Desktop/Order_Shop_Frontend/src/app/features/assistant/customer-chat/`: `chat-message.ts` reads Catalog's `images` and promotion fields, keeps legacy image support, selects an available variant and never invents missing prices/stock; `customer-chat.html` displays View Details, promotion, current price and stock, plus Add to Cart when stock is known and positive; `customer-chat.ts` keeps explicit authenticated cart actions using the existing Cart API. The catalog/order service remains authoritative for cart price, stock and ownership. Missing images use the existing placeholder asset.

## Examples

- `Find Nike shoes.` → query `Nike shoe`.
- `Show smartphones under $500.` → query `smartphone`, maxPrice `500`.
- `រកទូរស័ព្ទតម្លៃក្រោម ៣០០ ដុល្លារ` → query `phone`, maxPrice `300`.
- `Find in stock phones under $300` → query `phone`, maxPrice `300`, inStock `true`.

The deterministic vocabulary covers common phone, smartphone, shoe and laptop terms; other wording relies on the existing configured inference provider and validation. This is keyword search, not semantic translation of every catalog description.

## Regression coverage and deployment

Added Catalog/H2 repository integration tests for visibility, multiword and bilingual matching, literal wildcards, live promotion pricing, stock/image projections, invalid filters, pagination and result limits. AI orchestration tests cover validated forwarding, no results and downstream failure; inference tests cover bilingual extraction, tool allowlisting and complex-input fallback. Angular tests cover actual response fields, promotions, actions and unavailable stock.

The broader tests exposed a first-use quota race: assigned-ID JPA merge could reset an already-created quota counter. `ai-service/.../model/AiDailyQuota.java` now implements Persistable so a new row uses INSERT, allowing the existing constraint-conflict retry to lock the winning row. `AiQuotaServiceTest` verifies existing usage cannot be overwritten, alongside its concurrent-request test.

Deploy the updated Catalog service, AI service, inference service and Angular build together. No additional dependency or product-search database migration is required. The earlier `db/migrations/20261016_ai_execution_fingerprint.sql` remains a prerequisite for the routing changes. Builds/tests do not deploy or restart running services and do not certify external provider availability. Database filtering is covered with H2; PostgreSQL-only tests require their configured integration database.

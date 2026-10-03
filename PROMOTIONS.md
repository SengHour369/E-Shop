# Product promotions and checkout

## Architecture and compatibility

Catalog owns promotions, SKU assignments, pricing, inventory reservations and promotion usage.
Order owns orders and immutable line-price snapshots. Cross-service references are scalar IDs.
The original ProductSku.price and existing response price field remain base prices.
Product SKU responses add originalPrice, finalPrice, discountAmount, discountPercentage,
hasPromotion and a structured promotion result. Full product pages use six queries for
both 2-product and 20-product test pages, including pagination and live promotion pricing
(previously eight). SKU/inventory and attribute/value rows are loaded together, while
product images use URL projections without initializing entity collections. Stock and
promotion results are not cached; response fields and pagination remain unchanged.

For existing catalog databases with schema validation, apply
src/main/resources/db/upgrade/20261003_product_read_indexes.sql before deployment.
It adds indexes for the batch lookups on product_skus, attributes and images.
Development Hibernate schema updates also create these indexes. Query-count tests
measure database round trips, not end-to-end latency; actual response time still depends
on page size, SKU counts, database load and network latency.

Existing cart unit_price input is accepted but ignored. Checkout always reprices the complete
basket through Catalog. New orders contain baseUnitPrice, discountAmount (per unit),
finalUnitPrice, promotionId and promotionName. Existing unit_price is the paid unit price.
Promotion edits never change historical orders.

## Policy

All prices use USD, BigDecimal and two decimal places with HALF_UP rounding.
Percentage discounts must be above zero and at most 100; fixed discounts must be positive.
Caps apply per unit. minimumOrderAmount applies to the whole basket's base subtotal.
Storefront prices omit basket-conditional promotions because the basket is unknown.
Cart display is indicative; checkout calculates the complete basket authoritatively.

The lowest resulting unit price wins. Ties use higher priority, then lower promotion ID.
No stacking is supported; stackable=true is rejected. A zero saving does not consume a usage.
Each promotion counts once per order, including when multiple units or SKUs use it.
Null usage limits mean unlimited; zero means no uses. Cancellation releases the allowance.
Anonymous storefront prices cannot guarantee a customer's remaining personal allowance.

Promotion timestamps are timezone-less ISO values interpreted in app.time-zone (default UTC).
Set the same business zone on every catalog instance and send timestamps in that zone.
BaseEntity audit timestamps retain the application's existing JVM-local behavior.
Drafts can be edited. Published promotion terms are immutable; create a new promotion for
changed terms. Assignments can be added/removed under the promotion lock.

Draft -> Scheduled -> Active -> Expired. Disabling is permanent and never deletes history.
A scheduled promotion inside its time window has effective ACTIVE status even before the
scheduler persists that transition. Every pricing decision checks enabled state and
startAt <= now < endAt. The scheduler is idempotent and is not the pricing authority.

## APIs

Admin authority ADMIN is required, following Catalog's existing JWT role checks.
No new permission database or cross-service JPA permission relation is introduced.

| Method | Path under /api/v1 | Action |
| --- | --- | --- |
| POST | /admin/promotions | Create draft |
| PUT | /admin/promotions/{id} | Edit draft |
| GET | /admin/promotions | Filter by status, page (1-based), size (1..100) |
| GET | /admin/promotions/{id} | Details |
| DELETE | /admin/promotions/{id} | Soft-disable |
| POST / GET | /admin/promotions/{id}/skus | Assign / list SKU IDs |
| DELETE | /admin/promotions/{id}/skus/{skuId} | Remove assignment |
| POST | /admin/promotions/{id}/schedule | Schedule, or activate if already started |
| POST | /admin/promotions/{id}/activate | Activate inside the time window |
| POST | /admin/promotions/{id}/disable | Disable |
| GET | /promotions/active | Public active promotions |
| GET | /promotions/{id}/products | Public products under an effective promotion |

Create body example (adjust dates):

```json
{
  "name": "Weekend sale",
  "code": "WEEKEND-2026",
  "promotionType": "FLASH_SALE",
  "discountType": "PERCENTAGE",
  "discountValue": 20,
  "maxDiscountAmount": null,
  "minimumOrderAmount": null,
  "startAt": "2026-10-02T00:00:00",
  "endAt": "2026-10-05T00:00:00",
  "priority": 10,
  "usageLimit": 100,
  "usagePerCustomer": 1,
  "stackable": false
}
```

Assign with {"skuIds":[501,502]}. Use the existing product endpoints for effective prices.
POST /api/v1/orders/user/from-cart retains address_id, payment_method and currency,
and adds checkout_key. Send a stable unique key on retries. Omission generates a key
server-side, so a supplied key is recommended for retrying a completed checkout.
CHECKOUT_PENDING means recovery is still in progress; FAILED means validation failed.
The cart is retained and locked while processing, cleared on success, unlocked on failure.

## Transaction and recovery design

1. Order locks the cart and durably creates CHECKOUT_PENDING with its SKU/quantity snapshot.
2. Catalog reserves against order ID: locks promotion rows in ID order, rechecks limits
   and time, calculates prices, reserves inventory and writes usage in one transaction.
3. Catalog confirms idempotently: converts reserved stock to sold stock and records movements.
4. Order stores catalog snapshots, switches to PENDING and clears/unlocks the cart.

The order row is a durable recovery work item. If a response is lost, replay returns the
same reservation/snapshot rather than charging another use. Confirmation honors the
reserved price if the promotion subsequently expires or is disabled. This is the price
acceptance boundary; prices displayed before checkout do not reserve a price.
Reservations are not automatically expired: a timeout could otherwise free stock after
Order has accepted the catalog confirmation. Recovery must remain enabled.

Cancellation durably flags release work; the recovery job restores stock and allowance
idempotently. No distributed transaction is assumed. Catalog's pricing component does
not mutate inventory; the separate checkout coordinator owns reservation operations.
Inventory versioning prevents older admin writes from overwriting concurrent reservations.
Database constraints reject duplicate SKU inventory and invalid quantities.

Internal /internal/catalog/checkouts endpoints require a signed SERVICE_ORDER JWT and
are not routed through the public gateway. Order uses the existing shared JWT trust model,
with short-lived service tokens. Keep the shared secret private and restrict service ports
at deployment. End-user tokens now include userId; sign in again after upgrading.
Submitted customer IDs must match that signed claim for cart and checkout operations.

## Deployment

The project has no Flyway/Liquibase runner. SQL is explicitly versioned under each
service's src/main/resources/db/upgrade directory; it is NOT automatically executed.
For an existing database: stop writers, back up, run each 20261001 script once in its
own database, then deploy. Scripts are transactional and deliberately fail on incompatible
data. Do not apply these upgrade scripts after Hibernate has already created their columns.
For a fresh development database the existing ddl-auto=update bootstraps entities.
Production should use a reviewed schema baseline plus these scripts and ddl-auto=validate.

Automated tests include H2 integration coverage and an optional PostgreSQL suite.
To run PostgreSQL verification, use a disposable database named promotion_verification,
set ESHOP_TEST_POSTGRES_URL and ESHOP_TEST_POSTGRES_PASSWORD, and run
PostgresPromotionIntegrationTest. This suite creates/drops its schema; never point it at
a real application database.

Set CATALOG_SERVICE_URL for Order (Compose sets http://catalog-service:8082).
Use the same JWT_SECRET as Auth and Catalog. Keep promotion and checkout recovery jobs
enabled. Monitor CHECKOUT_PENDING orders and repeated recovery warnings; investigate
persistent failures before manually releasing reservations.
Failed recovery attempts back off for 30 seconds so they do not repeatedly occupy the
entire 50-order recovery batch. Internal catalog calls use bounded connection/read timeouts.

Payment initiation and shipping-address ownership verification were pre-existing TODOs in
Order and remain outside this module. PENDING means a priced, stocked order; it does not
mean payment succeeded. No promotion cache is introduced.

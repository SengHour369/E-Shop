# Single-page customer checkout — 2026-10-17

## Implementation

Frontend (`/home/senghour/Desktop/Order_Shop_Frontend`):

- `src/app/features/checkout/checkout.html`, `checkout.page.ts`, new `checkout.scss`: removed the three-step indicator and step state. Address, delivery information, available payment methods and order summary are visible together. Address creation/editing is inline; deletion requires an explicit confirmation. Desktop summary is sticky; phone Place Order bar is fixed. No order is submitted by selecting an address or payment method.
- `src/app/features/catalog/catalog.api.ts`: retrieve real product name, image and SKU attributes for each cart line. Displayed payable amounts remain the cart's authoritative values, independent of this presentation lookup.
- `src/app/app.ts`, `src/styles.scss`: checkout route class hides the normal phone navigation while the checkout action bar is visible.
- `src/app/features/checkout/checkout.page.spec.ts`: validates single-page sections, explicit submission, saved address and payment validation, reviewed totals/line quantities, one in-flight checkout, original-key recovery, inline addresses and refresh after definitive failure.

Backend (`/home/senghour/Desktop/E-Shop`):

- `catalog-service/.../controller/CheckoutProductController.java`: GET `/api/v1/products/sku/{id}` reuses ProductResponseService to return the actual active, non-deleted product containing a SKU. Existing security still requires authentication on this route. Missing/inactive/deleted products are rejected.
- New `CheckoutProductControllerTest` plus existing ProductReadQueryTest cover projection/rejection and real persistence reads.
- `compose.frontend.yaml`, `run-docker.sh`: repeatable frontend rebuild/start using the existing API Gateway network and http://localhost:4200.

Checkout recovery preserves the saved idempotency key and original selections after an uncertain response. Newly submitted orders include reviewed SKU quantities and total for server revalidation. Explicit terminal failure clears that attempt and disables placing another order until refreshed data is reviewed.

## Validation and deployment

- Selected frontend checkout/assistant suites: **65 passed**, 13 test files. After the phone navigation adjustment, the checkout subset passed **9 tests** again.
- Catalog controller/read suites: **6 passed**, Maven package succeeded.
- Existing launcher and Compose suites: **8 passed**.
- Production Angular and Catalog Docker builds succeeded. Angular emitted the existing initial-bundle warning (about 702 kB versus 650 kB).
- Recreated only frontend and Catalog. Frontend Docker health is healthy. Catalog OpenAPI is reachable through the gateway, including the new SKU presentation route.
- Applied the existing `db/migrations/20261017_catalog_approved_updates.sql` to the local Catalog database after startup showed that the existing category rows required a defaulted version column. No customer/order data was removed.

## Limits

The browser access policy blocked live page inspection, so no browser screenshot or visual end-to-end result is claimed. Angular component rendering, build checks and Docker/API readiness were verified. No order was placed during verification.

The current local Catalog database has no products, preventing a positive live SKU lookup or a populated real-cart checkout test. Component tests use fixtures; production UI uses actual backend data and an explicit SKU fallback if catalog presentation cannot load.

The reference image shows several shipping prices, coupons and card brands. Existing backend support does not provide shipping quotes, coupon application or card acquiring, so the UI does not fabricate these choices or charges. It displays delivery information and the payment methods actually returned by Payment Service. Existing shopper styling and authentication/order/payment protections remain in use.

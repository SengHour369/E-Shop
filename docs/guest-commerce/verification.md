# Verification — 19 October 2026

All commands ran locally. Customer rows, live commerce settings and deployed shop services were not modified by these implementation checks. Temporary verification containers were removed.

## Passing checks

| Check | Actual result |
| --- | --- |
| Angular production build, `npx ng build` | Passed. Existing initial bundle budget warning: 735.60 kB versus 650 kB. |
| Focused Angular/Vitest run | 49 tests passed across 10 files. Includes guest API/CSRF/scoped credentials, persisted settings, product reviews, registered checkout attempts, payment QR handling, cookie-session persistence, product cards and admin order details. |
| All-module Java package, Java 17, `mvn -q -DskipTests package` | Passed, including downstream compilation against nullable guest commerce references. This command compiles tests but does not execute them. |
| Affected backend module suites | Gateway, auth, catalog, order, payment, notification, admin and common-lib suites completed without assertion failures in the completed module runs. See focused test inventory below. A later full-root test attempt was interrupted, so it is not represented as a completed full-reactor test run. |
| `python3 scripts/verify-guest-migrations.py` | Passed against schema-only copies in an isolated PostgreSQL 18 container: admin (1 file), order (1), catalog (1), payment (1), auth (2). All six files applied twice; ownership, payment/address dependencies, unique guest carts and duplicate checkout constraints held. |
| PostgreSQL stock/pricing integration | 8 `PostgresPromotionIntegrationTest` tests passed with a disposable `promotion_verification` database. Includes concurrent guest reservations without overselling, guest owner isolation, per-customer promotion protection and existing customer promotion tests. |
| `python3 scripts/verify-guest-application.py` | Production order-service classes booted with H2, real disposable Redis and a local settings HTTP fixture. Actual HTTP checks passed for secure cookie flags, repository discovery, owned empty cart, trusted Origin, CSRF, private customer API protection and immediate feature disable. Test classes were excluded from the runtime classpath. |
| `git diff --check`, Python syntax checks and sample JSON parse | Passed. |

Focused backend additions:

- `GuestCommerceIntegrationTest`: 10 passing tests, including hashed/rotated/expired cookies, CSRF/Origin rejection, guest cart isolation, server-owned pricing, duplicate order protection, required address validation, contact/settings gates, price-change compensation, scoped order ownership and payment disable blocking QR initiation/images.
- `GuestOrderMailPrivacyTest`: 4 passing tests covering unverified recipient exclusion, authoritative recipient lookup, payment evidence and event deduplication. SMTP is mocked; no real email is sent.
- `ReviewModerationIntegrationTest`: 3 passing tests covering unpublished pending/rejected reviews, approval without fabricated purchase badges and duplicate review prevention.
- `StorefrontAccessTest`: 3 passing tests covering safe public defaults, persisted settings, admin permission checks and mandatory checkout/payment/address dependencies.
- `OrderAutomationTest`: added verified guest payment handling through the existing inventory confirmation path; customer provider-verification tests remain in place.

## Commands

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -q -pl admin-service,auth-service,order-service,payment-service,catalog-service,notification-service,api-gateway -am test
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -q -pl order-service,payment-service,notification-service,admin-service -am test
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -q -DskipTests package
python3 scripts/verify-guest-migrations.py
python3 scripts/verify-guest-application.py
```

The PostgreSQL integration class requires `ESHOP_TEST_POSTGRES_URL` ending in `/promotion_verification` and `ESHOP_TEST_POSTGRES_PASSWORD`; it creates/drops its schema. Only use a disposable database. It was run with:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -q -pl catalog-service -am test -Dtest=PostgresPromotionIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Frontend command, run from Order_Shop_Frontend:

```bash
npx ng test --watch=false --runner-config=vitest.config.mts \
  --include='src/app/core/storefront/*.spec.ts' \
  --include='src/app/features/admin/settings/storefront-access-settings.spec.ts' \
  --include='src/app/features/catalog/reviews/product-reviews.spec.ts' \
  --include='src/app/features/checkout/*.spec.ts' \
  --include='src/app/features/payments/*.spec.ts' \
  --include='src/app/features/admin/orders/order-detail.spec.ts' \
  --include='src/app/core/auth/session-persistence.spec.ts' \
  --include='src/app/shared/components/product-card.spec.ts'
```

## Existing test limitations and work not represented as verified

- Resolved 20 October 2026: stale customer-assignment fixtures were updated to the current form; the complete frontend suite now passes (460 tests, 80 files).
- Resolved 20 October 2026: API-contract fixtures now use session login; logout preserves its original legacy bearer header after clearing local session state.
- The auth refresh-cookie unit fixture was missing its existing FunctionPermissionRepository dependency. The fixture now provides that mock; token implementation was not changed.
- Provider success/expiration/failure evidence is covered by the existing controlled-response payment tests. No real KHQR transfer, new provider integration, real SMTP delivery or complete multi-service browser purchase was performed.
- Updated 20 October 2026: guest migrations and the compatible services are deployed locally. Guest features remain disabled in saved settings. No real payment or SMTP delivery has been verified.

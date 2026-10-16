# Shopping assistant validation — 2026-10-17 (Asia/Phnom_Penh)

## Results

| Check | Result |
|---|---|
| Relevant Java suites across Auth, Catalog, Order, Payment, AI and common import code | **253 passed, 12 skipped**, across 52 test classes; no failures/errors |
| Python inference, search, vision and mapping suites | **103 passed** |
| Angular assistant, admin assistant/import and cart suites | **130 passed**, 16 test files |
| All backend modules: Maven package | **BUILD SUCCESS**, all 11 reactor projects |
| Angular production build | **Passed**; initial bundle 702.23 kB, exceeding the 650 kB warning budget by 52.23 kB |
| Scoped whitespace/diff validation | Passed |

The first backend run was interrupted after the selected Auth, Catalog and Order suites completed. Payment and AI were rerun successfully. The final whole-reactor packaging run also executed OrderAutomationTest and AiKhqrOwnershipTest. Counts above merge completed per-class results and do not double-count repeated suites.

The skipped classes are AiQuotaPostgresTest (10 tests) and AiTrainingDataSeederPostgresTest (2 tests), which require explicitly configured isolated PostgreSQL test environments. H2 quota/concurrency and workflow tests passed; PostgreSQL-specific behavior is not certified by this run. Python emitted an existing Starlette TestClient/httpx deprecation warning. Maven emitted existing Lombok/deprecation warnings.

## Coverage that passed

- Canonical tool schemas, unsupported tools, arbitrary endpoint rejection, invalid nested/duplicate JSON, finite numeric validation, English/Khmer intent and Khmer numerals.
- Live permission checks, JWT identities, owner-scoped orders/payments/addresses/returns, revoked cache access, guest context isolation and request fingerprint conflicts.
- Real H2-backed Catalog search/filtering, active/deleted product exclusion, actual SKU price/promotions/availability and bounded search pagination.
- Image upload identity binding, inference failures, catalog candidate-only ranking, Cloudinary URL restriction, nonfinite score rejection and explicit visual fallback.
- Inline product galleries/variants and camera upload UI, explicit confirmation, unknown cart-response retry keys, owned order controls and authoritative payment states.
- Atomic cart mutation receipts, repeated-key handling, fresh basket promotion quoting, review ownership, stale cart/price rejection, address ownership, inventory failures and duplicate-order prevention.
- KHQR rendering, trusted verification policy, wrong recipient/unverified/mismatched payment refusal, expiry and failure handling, duplicate verified events and late payment conflicts.
- Admin draft schema/domain permissions, staged workbook ownership and no execution before approval, import review/approval safeguards, stale resource versions and update receipt replay.

## Reproduction

Run Maven with Java 17 from `/home/senghour/Desktop/E-Shop`. Use the locally installed Node runtime from the Angular repository `/home/senghour/Desktop/Order_Shop_Frontend`.

```bash
mvn -B -ntp -pl auth-service,ai-service,order-service,payment-service,catalog-service -am '-Dtest=Ai*Test,Import*Test,ExcelImportParserTest,Address*Test,CheckoutIntegrationTest,CustomerCancellationTest,ProductReadQueryTest,CustomerReturn*Test,ProductScanner*Test,ProductImageConfirmationTest,HttpProductVisionClientTest,CatalogImageMatcherTest,CatalogVisualComparisonTest,PromotionPricingTest,PromotionIntegrationTest,CatalogImportServiceTest,PaymentAutomationTest,KhqrRenderingTest,BakongVerificationPolicyTest,OrderAutomationTest' -Dsurefire.failIfNoSpecifiedTests=false package
mvn -B -ntp '-Dtest=AiKhqrOwnershipTest,OrderAutomationTest' -Dsurefire.failIfNoSpecifiedTests=false package
python -m pytest -q ai-inference/tests
```

```bash
npm test -- --include='src/app/features/assistant/**/*.spec.ts' --include='src/app/features/admin/assistant/*.spec.ts' --include='src/app/features/cart/**/*.spec.ts' --include='src/app/features/admin/imports/**/*.spec.ts'
npm run build
```

Raw logs are retained locally under `/home/senghour/.cache/eshop-ai-validation/`: `backend-tests.log`, `backend-remaining-tests.log`, `reactor-build.log`, `python-tests.log`, `frontend-tests.log`, and `frontend-build.log`.

## Limits and deployment

These checks exercised actual application handlers and persistence logic in isolated tests, with external AI/payment/notification endpoints mocked where appropriate. They did not place live orders, charge a bank account, apply database migrations, restart services, or establish live provider credentials. The currently running application must be deployed with the new artifacts and reviewed migrations before exposing the new tools.

See [AI_SHOPPING_PLATFORM_GAPS.md](AI_SHOPPING_PLATFORM_GAPS.md) for changed paths, required migrations/configuration, and unresolved business decisions. Return creation remains disabled pending a defined return policy and domain hardening. Live KHQR settlement and configured visual ranking require separate operator-controlled verification.

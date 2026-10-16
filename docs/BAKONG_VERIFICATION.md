# Bakong / individual KHQR activation — 2026-10-17

## Changes

- Added explicit `BAKONG_KHQR_TYPE=INDIVIDUAL` using the existing NBC SDK's `generateIndividual`. The configured recipient account is authoritative; merchant ID and acquiring-bank fields are not required in this mode. MERCHANT mode retains its validation.
- Individual QR generation includes the server-calculated order amount, bill number, expiry and purpose. It does not copy invented default merchant IDs, phone numbers or bank values from the legacy request DTO. Its display label is the account identifier's local part; optional city label defaults to `KH`.
- Payment method availability and automation configuration now understand both KHQR modes. Unknown modes fail Spring property conversion.
- Docker Compose forwards the Bakong configuration. `run-docker.sh` loads the automation overlay when its resolved private environment explicitly enables `COMMERCE_AUTOMATION_ENABLED`.
- Corrected the local registered email to `seanghour097328@gmail.com` after owner confirmation; selected INDIVIDUAL mode in ignored `.env.local`. The original tracked legacy `env` was preserved.
- Excluded `env` and `.env.*` from Docker build contexts.

## Actual local activation and checks

Before enabling workers, the local databases contained no orders or payment records and Kafka had no commerce topics/backlog. Applied the existing Order and Payment `src/main/resources/db/upgrade/20261011_commerce_automation.sql` migrations to their respective local Docker databases. Also applied `db/migrations/20261016_ai_shopping.sql` to local Order. These additive migrations completed without errors.

Rebuilt and recreated only Order and Payment with the automation overlay and corrected private settings. Both started successfully. Gateway OpenAPI returned HTTP 200 for both services and exposed their automation routes. The payment-methods route is present. Kafka created commerce order/payment topics and their dead-letter topics. No orders or payments were created to test activation.

Live provider checks:

1. Bakong HTTPS root responded HTTP 200 (connectivity only).
2. The supplied `gamil.com` email initially returned HTTP 200, responseCode=1, errorCode=10, “Not registered yet.”
3. After the owner's correction to Gmail, `/v1/renew_token` returned HTTP 200, responseCode=0, and a token. The token was retained only in test-process memory and never printed or written to a file.
4. An authenticated `/v1/check_transaction_by_md5` lookup for an all-zero test hash returned HTTP 200, responseCode=1, errorCode=1 (not found), proving API authentication without creating a payment or claiming a successful transaction.

## Tests and limits

- **28 Java tests passed** across payment/order security, verification policy, failure/expiry handling, KHQR SDK rendering and order automation. Maven package succeeded.
- Individual SDK tests decoded the generated QR and checked configured recipient, actual amount, absent merchant ID/acquiring bank, valid CRC, MD5 and PNG output. Merchant mode regression tests also passed.
- **8 launcher/Compose tests passed**, including the explicit commerce flag and merchantless individual configuration. Shell syntax and scoped diff checks passed.

This verifies configuration, QR construction, provider authentication and negative transaction lookup. It does not certify an actual settled bank transfer, callback/deep-link URLs, an entire authenticated browser checkout, or PostgreSQL tests that require a separate test database. Order payment completion still requires matching amount, currency, recipient and trusted provider transaction evidence; screenshots and chat text cannot mark an order paid.

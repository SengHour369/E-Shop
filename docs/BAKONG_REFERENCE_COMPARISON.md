# Comparison with the owner's working Order_Shop payment flow

Reviewed on 2026-10-17. Source: https://github.com/SengHour369/Order_Shop/tree/main/Backend/src/main/java/com/example/learning_spring_security/Bakong/service/impl

The directory contains another nested `service/impl`; the actual service files are `service/impl/BakongServiceImpl.java` and `service/impl/BakongTokenServiceImpl.java`. Also reviewed the reference BakongController, request DTO, OrderServiceImpl's QR/verification/callback methods and related frontend callback hooks.

## Reused protocol

E-Shop already uses the reference's core Bakong protocol:

- POST `/v1/renew_token` with the registered email; cache its token until the expiry safety window.
- Generate an amount-bound KHQR using the NBC SDK and retain its QR MD5 for verification.
- POST `/v1/check_transaction_by_md5` with Bearer authentication and the stored MD5.
- Generate QR PNG output with the existing QR renderer.

The reference uses merchant KHQR and fixed shop labels/merchant fields. E-Shop keeps merchant mode available and uses INDIVIDUAL mode locally in accordance with the owner's earlier instruction to remove mandatory merchant ID/acquiring-bank fields. Both use the same Bakong token and transaction verification endpoints. No runtime payment protocol change was necessary after this comparison.

## Payment authority

The reference's callback method accepts a submitted status and updates payment/order state from it. E-Shop retains authoritative provider verification, including amount, currency, recipient and transaction hash checks, plus duplicate-event protection and order ownership. The reference's client-supplied status path is not used as proof of payment. The original user reports successful bank payment testing in Order_Shop; that report is distinct from the checks performed in this workspace.

## New validation

Added `payment-service/src/test/java/com/example/eshop/payment/BakongClientContractTest.java` with four wire-contract tests:

1. Correct token endpoint, email JSON, content type and caching.
2. Correct MD5 endpoint, Bearer header and JSON, with provider evidence passed into amount/recipient validation.
3. Transaction-not-found remains unverified/unpaid.
4. Unregistered email cannot yield a verification token.

Ran these tests together with the existing SDK QR rendering, ownership, verification policy, payment automation and order automation tests: **32 passed, no skips or failures**. Maven package succeeded. The four new wire-contract tests use a mocked HTTP transport; they do not make a bank transfer.

The earlier live provider authentication and negative lookup remain documented in [BAKONG_VERIFICATION.md](BAKONG_VERIFICATION.md). No real transfer or customer order was created during this comparison, and no service restart was needed for these test-only additions.

# Bakong app payment

The existing customer chatbot and checkout UI are retained. Product search, SKU/stock checks, saved default delivery address, cart price review and explicit final checkout confirmation remain in their existing services. A request mentioning Bakong/KHQR preselects the available KHQR method in the chat checkout; the customer can review it before confirming. The order is created with the existing checkout idempotency key. A pending QR order is retained in CheckoutAttempt for recovery after leaving chat.

## App handoff

`POST /api/v1/payments/orders/{orderId}/bakong-app` requires the authenticated order owner. PaymentAutomation checks the authoritative order amount and currency, then validates the stored dynamic QR through the NBC KHQR SDK: checksum, QR MD5, recipient account, amount, USD currency code, ORDER-{id} bill reference, and registered merchant ID in MERCHANT mode. Only pending, unexpired payments on pending orders can be handed off.

BakongAppPayment calls the configured provider's `/v1/generate_deeplink_by_qr` endpoint using the existing server-side Bakong token service. It sends the stored QR and fixed sourceInfo. Only HTTPS provider-returned short links on the configured exact hostname allowlist are returned. No arbitrary callback URL or QR is accepted from the browser or AI. An audit entry records the handoff without credentials, QR payload or provider response. POST returns Cache-Control: no-store.

The payment component shared by chatbot and checkout presents Pay with Bakong App, followed by a normal Open Bakong App anchor after the provider returns the link. Opening uses an explicit user gesture, avoiding unreliable async popup launching. The browser cannot reliably detect whether Bakong is installed, so the existing QR remains available on mobile and desktop. Link failures do not create another payment or clear checkout.

## Verification and return

The fixed HTTPS return route is `/checkout/payment/{id}?bakongReturn=1`. This route requires sign-in and reads the owned order. The return parameter is informational and never authorizes payment or changes a payment status. Existing provider verification runs periodically; returning to a visible payment screen triggers an additional verification request. A new callback page polls the same verification endpoint. No callback endpoint accepts a claim of success.

The existing trusted MD5 lookup independently validates recipient, currency, amount and provider transaction hash. The verified transaction has a unique provider hash; a database lock protects the final transition and Kafka outbox publication. Existing PaymentTransaction uses SUCCESS (not a new COMPLETED enum); the related Payment aggregate uses COMPLETED. OrderAutomation consumes PAYMENT_VERIFIED, fetches the authoritative payment evidence and moves the order through confirmation into PROCESSING, retaining paymentId. This preserves the existing order state machine rather than introducing an incompatible PAID state.

Pending and failed attempts are reconciled using existing verification, expiry and order-restoration behavior. A valid QR is reused; an expired or cancelled QR cannot be launched. Do not regenerate an overlapping payment session while an earlier outcome is unknown. After independently confirmed failure, the existing Return to Checkout flow restores order items and delivery details for a fresh review.

## Deployment configuration

Configure through the existing merchant secret/environment mechanism; never put tokens in Angular:

- BAKONG_APP_PAYMENT_ENABLED=true
- BAKONG_RETURN_ORIGIN: the merchant's approved public HTTPS storefront origin, without path, query or fragment
- BAKONG_APP_ICON_URL: the public HTTPS merchant application icon
- BAKONG_ALLOWED_LINK_HOSTS: comma-separated exact hostnames approved by NBC for the provider's returned short links; no wildcards
- Existing BAKONG_BASE_URL, BAKONG_EMAIL, BAKONG_ACCOUNT_ID, KHQR merchant configuration remain authoritative.

These variables are passed through compose.yaml and compose.automation.yaml. Defaults keep app handoff disabled, with QR checkout working independently. A LAN IP and untrusted local TLS certificate are insufficient for a production provider return link. Do not enable production payments until the merchant's provider configuration and a real mobile return/verification test are complete.

## Reference

NBC KHQR SDK documentation describes provider-generated KHQR deep links and sourceInfo (app icon, app name, callback): https://bakong.nbc.gov.kh/download/KHQR/integration/KHQR%20SDK%20Document.pdf

## Verification performed

Automated tests exercise provider request shape, ownership denial, invalid return configuration, exact hostname filtering, expired/completed handoff rejection, QR identity checks, existing payment verification/replay, explicit frontend handoff, QR fallback, checkout recovery and continued provider-only success. No live transfer or real merchant app launch is performed by these tests.

The owned QR response also includes `bakongAppAvailable`. When app payment is disabled or return/host configuration is missing, the customer payment component explains the QR fallback and omits the app preparation button. It does not issue failing app-link requests. HTTP 200 from verification means the verification request completed; the returned provider-backed payment state remains the source of truth.

## HTTPS IP mode (2026-10-25)

A fresh QR generated by the installed official SDK was sent to the production provider during configuration diagnosis, without creating an order, persisting a payment session, displaying the QR, or submitting a transfer. The authenticated `/v1/generate_deeplink_by_qr` request returned responseCode=0 and an HTTPS link on `bakong-deeplink.nbc.gov.kh` when sourceInfo was omitted. Adding the local HTTPS IP callback/icon metadata returned errorCode=4. Thus a working provider handoff does not require changing the storefront to HTTP.

`BAKONG_APP_SOURCE_INFO_ENABLED=false` selects the official optional-sourceInfo mode for local HTTPS IP testing. It omits callback/icon metadata, returns `automaticReturn=false`, and instructs the customer to switch back manually. Visibility-triggered and periodic trusted bank verification remain unchanged. No payment is considered successful based on app opening or returning. The configured return origin remains an HTTPS E-Shop route; it is not supplied to the provider in this mode.

The local settings now enable app payment with the exact hostname observed in the trusted provider response. No link itself is hardcoded. Keep sourceInfo enabled for a production integration whose public callback/icon metadata has been accepted by the provider. Browser/phone app opening still needs a real-device test and a current unexpired checkout; the earlier expired order QR must not be reused.

### Handoff audit transaction fix

The provider request must run outside a database transaction, but AuditLogService.record uses MANDATORY transaction propagation. BakongAppPayment now wraps only the audit insert in TransactionTemplate after receiving and validating the link. Previously, a successful provider response could still become HTTP 503 because the audit call had no active transaction. The handoff regression tests assert that an active transaction exists at the audit boundary. Failure diagnostics log only order ID and exception class, never tokens, QR data or provider links.

## Handoff audit transaction

AuditLogService.record uses transaction propagation MANDATORY. Handoff auditing therefore runs inside a short TransactionTemplate transaction after the provider has returned an accepted link; the provider HTTP call itself remains outside the database transaction. A successful provider response must not be turned into an unavailable response by a missing audit transaction. Handoff tests assert an active transaction inside the audit call. Failure logs record only order ID and exception class, excluding link URLs, QR contents and credentials.

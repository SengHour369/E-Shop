# Checkout service authentication fix — 2026-10-18

## Failure and fix

Request `1a74a609-e604-4e70-bc94-3085e1218cf8` failed while Order Service called Auth Service to check delivery-address ownership. The customer JWT filter tried to load `order-service` as a human account and rejected it. A second customer permission interceptor also rejected the internal commerce endpoint.

`InternalCommerceSecurityConfig` now uses the existing signed-claim JWT validator for `/internal/commerce/**` and requires `SERVICE_ORDER`. These routes are excluded from the customer permission interceptor, retaining the security-chain and controller authority checks. Human authentication behavior is unchanged.

Auth Service was rebuilt and recreated. The running Order Service lacked `COMMERCE_AUTOMATION_ENABLED`, although the existing automation Compose overlay enables it. Order Service was recreated using the existing deployment files and environment files. Existing automation tables were confirmed present; no migrations or stock updates were applied.

## Automated verification

Run using Java 17 from the backend repository:

```sh
mvn -B -ntp -pl auth-service -am -Dtest=InternalCommerceSecurityTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Four tests passed. Coverage includes signed Order Service calls, unowned addresses, absence of customer permission interception on the machine route, wrong authorities, invalid signatures and missing tokens.

## Real HTTP verification

See [sanitized HTTP results](checkout-service-auth-http-2026-10-18.json).

- Direct Auth Service ownership check: HTTP 200, `true` for address 1/user 1.
- Wrong service authority: HTTP 403.
- Missing token or invalid signing key: HTTP 401.
- Real admin login through `http://192.168.1.10:41075`: HTTP 200.
- Original checkout payload and key through the same frontend/gateway address: HTTP 201, order 1, business status `FAILED`.
- Same-key retry: HTTP 201 and the same order ID; no duplicate order.
- QR lookup: HTTP 400; no QR issued for the failed order.

A read-only Catalog HTTP quote confirmed 3 units of SKU 1 cost USD 3. A read-only inventory query confirmed only 1 unit in stock, with 0 reserved. The order failure is therefore consistent with the inventory guard. HTTP 201 alone must not be interpreted as successful checkout; callers must inspect order status.

The original HTTP 500 has been resolved.

## Authorized in-stock checkout verification

The owner authorized changing the cart to 1 unit and creating a pending Bakong order. All operations used the real HTTP API at `http://192.168.1.10:41075`.

See [one-unit HTTP results](checkout-one-unit-http-2026-10-18.json).

- Cart update to 1 unit: HTTP 200.
- New checkout with expected total USD 1.00: HTTP 201, order **2**, status **PENDING**.
- Same-key retry: HTTP 201, same order ID.
- Order QR metadata: HTTP 200, nonempty QR, payment status **PENDING**, USD 1.00.
- QR image: HTTP 200, `image/png`, valid PNG signature.
- Cart after checkout: HTTP 200, zero items.

The pending order reserves the available unit. No payment was submitted or marked successful; real merchant settlement was not tested.

# E-Shop Postman collection

Import `E-Shop-All.postman_collection.json`, then import and select one environment:

- `E-Shop-Local-Bearer.postman_environment.json` for bearer-token requests.
- `E-Shop-Local-Cookie.postman_environment.json` for browser-style cookie requests.

The collection contains **188 requests**, covering **174 current controller mappings**
across auth, catalog, order, payment and gateway modules. It also includes the legacy
security-filter login, six product-filter examples, gateway health/info and OpenAPI requests.
The older root collections are retained but may contain obsolete endpoints.

## Start with authentication

1. Start the gateway and the services you want to test. Keep `baseUrl` as
   `http://localhost:8080`, or replace it with your gateway URL.
2. Set the environment's `username`, `password` and `email` to your test account.
   Credentials and tokens are intentionally empty or illustrative in the exported files.
3. For bearer mode, run **auth-service → Login → Login by email**.
   Successful login saves `accessToken`, `refreshToken` and `userId` and selects bearer mode.
4. For cookies, run **auth-service → Session → Login**. Postman saves the HttpOnly cookies
   in its cookie jar; the script selects cookie mode. Do not disable Postman's cookie jar.
5. Run requests from the relevant service folder. Set resource IDs in the environment
   to records that exist in your database. IDs default to `1` as placeholders.

The collection adds Authorization only in bearer mode and includes `Origin: {{origin}}`.
Successful login switches the selected environment's `authMode` automatically.
Clear the cookie jar when switching accounts or changing from cookie to bearer testing.
Requests do not copy HttpOnly cookies into JavaScript-accessible token variables.

For local HTTP cookie testing, set `AUTH_COOKIE_SECURE=false` on auth-service and restart
it. Keep secure cookies enabled for HTTPS. `origin` must exactly match an entry in
`GATEWAY_CORS_ORIGINS` on auth and gateway; the example is `http://localhost:5173`.
Session refresh and logout use the refresh cookie and require no body. The bearer refresh
and logout requests use `refreshToken` in their JSON body instead.

## Modules

| Folder | Requests | Includes |
| --- | ---: | --- |
| auth-service | 56 | Registration, verification, bearer and cookie sessions, users, addresses, groups, permissions |
| catalog-service | 48 | Categories, icons, subcategories, products, attributes, values, inventory |
| order-service | 38 | Cart, orders, cancellations, returns, refunds and lifecycle actions |
| payment-service | 16 | Payments, transactions, status history and Bakong QR/status |
| api-gateway | 13 | Dynamic route administration, fallback diagnostic, health and API specifications |

Gateway administration requires gatewayAdminKey to match the key stored in the
gateway database. On startup, the gateway automatically creates the
gateway_admin_keys table and inserts a cryptographically random key if no row
exists. Subsequent starts reuse that row; simultaneous starts cannot overwrite it.

Using your database administration connection to gateway_db, retrieve the key:

    SELECT admin_key FROM gateway_admin_keys WHERE id = 1;

Set the Postman environment variable gatewayAdminKey to that value. Requests
send it as X-Gateway-Admin-Key. Keep the value private; it is not logged or
returned by the HTTP APIs.

GATEWAY_ADMIN_KEY is now an optional **initial seed** for an empty table, for
compatibility with existing installations. Once the row exists, the database
value wins even if that environment variable changes. To rotate, update the
database key securely and restart all gateway instances. Do not delete the row
during ordinary restarts. Gateway startup fails if key initialization cannot
complete within 30 seconds.

The table stores the recoverable credential, so restrict database/table and backup
access to trusted administrators and the gateway runtime. Existing installations
use the project's spring.sql.init.mode=always schema initialization. If SQL init
is disabled in deployment, apply the table definition from
api-gateway/src/main/resources/schema.sql before startup.

Create/update examples target `lb://recommendation-service`; that service must be registered
before the example route can forward traffic. The fallback diagnostic deliberately returns 503.

## Request data

- Each request description names its source controller and handler.
- JSON bodies use the current DTO field names. For example, cart `product_id` contains
  **a SKU ID**. The legacy `unit_price` field is ignored; Catalog supplies the price.
- Product creation sends multipart text fields, a JSON `skus` field and file inputs.
  Select real files in Postman's Body tab. Repeated file keys support multiple uploads.
  The SKU example includes inventory and enables attributes; update examples reference `skuId`.
- Product listing examples cover all products, active products, search, category,
  subcategory and both ID modes. Their JSON page numbers start at 1; maximum size is 100.
- Requests with Spring Pageable query parameters start at page 0.
- Optional query/form fields are included but disabled. Enable and fill them when needed.
- Date examples and amounts are illustrative. Payment, return, refund and order-state
  transitions require compatible existing records; the collection is not a seeded scenario.
- Fill `verificationCode`/`resetToken` from your test email. Fill `qr` and `md5` with actual
  QR/transaction data for Bakong calls. No real payment credentials are included.

Select and send the requests you intend to exercise. This is a full API reference, not a
single sequential runner scenario: it includes create, update, delete, stock, payment,
refund and logout operations. Default response tests expect HTTP 2xx except the fallback
diagnostic, which expects 503. Assertions report failures; they do not retry operations.

## Known backend limitations

- `POST /api/v1/user/id/update` declares `@PathVariable Long id` without an `{id}` URL
  segment. Its request is included and explicitly marked; the backend must be corrected
  before it can bind the ID. This Postman task does not change backend routes.
- Legacy `POST /api/v1/auth` returns a JWT refresh token, whereas `/public/refresh` expects
  a database-backed opaque refresh token. Prefer Login by email or Session login.
- Some endpoints require administrator authority, configured permissions, running services,
  or external providers. A valid token alone does not guarantee access.
- Obsolete Bakong webhook routes from the old collection are excluded because they have no
  current controller. SKU operations are nested in product requests; there is no SKU controller.

## Validation and regeneration

```powershell
python postman/generate_collection.py
python postman/validate_collection.py
```

`endpoint-coverage.json` records every discovered controller mapping, including the separate
multipart and JSON product-update mappings. Validation checks coverage, request structure,
variable definitions and JSON examples after substitution. Generation and validation do
not send requests, create records, or make payments. Live endpoint execution remains untested.
# Promotion and checkout requests

The collection now includes promotion administration, public active promotions, and internal
catalog checkout operations. Admin promotion requests require an ADMIN account.
Change the promotion dates before creating a sale; assign SKU IDs, then schedule or activate it.
Cart unit_price is now ignored. Send a stable checkout_key when retrying order creation.
Sign in again after upgrading so the token includes the verified customer ID.

Internal catalog requests use catalogBaseUrl and a separately supplied short-lived
SERVICE_ORDER token in serviceToken. They cannot be called through the gateway with a
normal customer/admin token. See ../PROMOTIONS.md for the lifecycle and deployment steps.


### Gateway admin key tests

The standard gateway build tests generation, existing-key reuse, concurrent
initialization, configured seeds, failure handling, and endpoint authorization.
An optional PostgreSQL test accepts GATEWAY_KEY_TEST_URL, GATEWAY_KEY_TEST_USER,
and GATEWAY_KEY_TEST_PASSWORD. Its database name must start with
gateway_key_verification_ and contain only lowercase letters/digits afterward.
Use a disposable database: the test creates tables from schema.sql.

During this implementation, the PostgreSQL schema and insert-if-absent SQL were
verified directly. The optional Java/R2DBC test could not connect because the
local Windows Java runtime failed to create a loopback selector.

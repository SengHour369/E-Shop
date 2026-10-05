# E-Shop flowchart

The browser and Postman talk only to the API gateway on port 8080. Each service keeps its own database. Redis is shared for the gateway rate limit and a startup ping. Stock and promotion prices are read from catalog on every request.

One image of the whole path: [full.png](flowchart/full.png).

Separate sheets: [services](flowchart/services.png), [gateway](flowchart/gateway.png), [account](flowchart/account.png), [buy](flowchart/buy.png), [scanner](flowchart/scanner.png), [AI](flowchart/ai.png), [audit](flowchart/audit.png).

## 1. Services

```mermaid
flowchart LR
  client[Browser or Postman]
  gateway[API gateway :8080]
  eureka[Discovery :8761]
  redis[(gateway-redis :6379)]
  auth[auth-service :8081]
  catalog[catalog-service :8082]
  orders[order-service :8083]
  payment[payment-service :8084]
  ai[ai-service :8085]
  notes[notification-service :8086]
  infer[ai-inference :8000]
  openai[OpenAI Responses]
  kafka[(Kafka)]

  client --> gateway
  gateway --> eureka
  gateway --> redis
  gateway --> auth
  gateway --> catalog
  gateway --> orders
  gateway --> payment
  gateway --> ai
  gateway --> notes
  orders --> catalog
  ai --> infer
  ai --> catalog
  ai --> orders
  infer --> openai
  catalog --> infer
  ai --> kafka
  kafka --> notes
  notes --> auth
```

Auth, catalog, order, and payment use the Postgres databases on ports 5432–5435. Gateway, AI, and notification use 5436–5438. Discovery and ai-inference do not use Redis.

## 2. Every request through the gateway

```mermaid
flowchart TD
  start[Request to http://localhost:8080]
  id[Keep a safe X-Request-ID or create one]
  pub{Public path or valid access token?}
  deny[401 Missing or invalid Authorization]
  strip[Remove caller identity headers and admin key]
  rate{Redis rate window}
  allow[Redis is down so the request is allowed]
  limited[429 too many requests]
  route[Match the path and ask Eureka for the service]
  ok{Service answers before the time limit?}
  body[Return the service status and body]
  down[503 service is temporarily unavailable]

  start --> id --> pub
  pub -->|no| deny
  pub -->|yes| strip --> rate
  rate -->|over the limit| limited
  rate -->|inside the limit| route
  rate -->|Redis error| allow --> route
  route --> ok
  ok -->|yes| body
  ok -->|timeout or open circuit| down
```

Public paths are registration, verification, login, refresh, password reset, session login, active promotions, and OpenAPI. Product, scanner, cart, order, payment, AI, and notification calls need a bearer token or the access cookie. The usual time limit is 6 seconds. Catalog's HTTP client limit is 5 seconds. AI is allowed 30 seconds. A slow first start can return 503; the same call succeeds on retry.

## 3. Account

```mermaid
flowchart TD
  reg[POST /api/v1/public/register]
  fields{username password email phone and full_name}
  bad[400 invalid body]
  taken[409 duplicate user or missing phone stored as a conflict]
  created[201 user created and verification mail queued]
  verify[POST /api/v1/public/verify]
  login[POST /api/v1/public/email/username/login]
  act{Account status is ACT?}
  rejected[401 invalid username or password]
  token[200 access token about 15 minutes and refresh token]

  reg --> fields
  fields -->|no| bad
  fields -->|duplicate| taken
  fields -->|yes| created --> verify --> login --> act
  act -->|no| rejected
  act -->|yes| token
```

Seed users must be stored as `ACT`. `ACTIVE` is rejected. Session login at `/api/v1/public/session/login` sets HttpOnly cookies instead of returning the tokens in JSON.

## 4. Buy a product

```mermaid
flowchart TD
  list[POST /api/v1/products/get/all]
  page[Catalog loads one page then images SKUs inventory and promotions]
  add[POST cart item]
  quote[Order asks catalog for the current price]
  save[Save the line and ignore the client unit_price]
  checkout[POST /api/v1/orders/user/from-cart]
  key{checkout_key already used?}
  same[Return the existing order]
  pending[Create order CHECKOUT_PENDING]
  reserve[Catalog reserves stock]
  fail[Order FAILED and the cart is kept]
  confirm[Catalog confirms and returns its prices]
  done[Order PENDING cart cleared]
  retry[Leave CHECKOUT_PENDING and retry]
  pay[POST /api/v1/payments or Bakong QR]

  list --> page --> add --> quote --> save --> checkout --> key
  key -->|same address| same
  key -->|new| pending --> reserve
  reserve -->|400 or 404| fail
  reserve -->|reserved| confirm
  confirm -->|prices copied| done --> pay
  confirm -->|timeout| retry --> reserve
```

Catalog is the only writer of stock and of the price used on the order. The client cannot send a product id, a price, or a stock count that the services trust. Payment is a separate call after the order is `PENDING`.

## 5. Product scanner

```mermaid
flowchart TD
  scan[POST /api/v1/product-scanner/scan]
  valid{Code and format valid?}
  invalid[400 invalid code or format]
  join[Join the same actor and code for 1.5 seconds]
  limit{More than 30 scans in 10 seconds?}
  tooMany[429 retry after 1 second]
  find[Lookup barcode then internal SKU]
  hit[200 found true with catalog price and stock]
  miss[200 found false]
  image[POST /api/v1/product-scanner/recognize]
  file{jpeg png or webp and at most 1.5 MB?}
  vision[ai-inference names candidates only]
  down[200 AI_UNAVAILABLE]
  exact{One catalog name and confidence at least 0.90?}
  one[Exact catalog product]
  many[Candidates and the caller must choose]

  scan --> valid
  valid -->|no| invalid
  valid -->|yes| join --> limit
  limit -->|yes| tooMany
  limit -->|no| find
  find -->|row exists| hit
  find -->|no row| miss
  image --> file
  file -->|no| invalid
  file -->|yes| vision
  vision -->|provider down| down
  vision -->|names returned| exact
  exact -->|yes| one
  exact -->|no| many
```

A scan does not create a product, change stock, or change `ProductSku.price`. The example code `8850123456787` is a valid EAN-13. Warehouse location is returned only to ADMIN, MANAGER, and STAFF.

## 6. AI request and notification

```mermaid
flowchart TD
  call[POST /api/ai/execute with message]
  replay{Same Idempotency-Key for this user?}
  saved[Return the saved status and do not run the tool]
  tools[List the tools this actor may use]
  route[POST /api/v1/ai/route on ai-inference]
  model[OpenAI selects one supplied tool]
  trust{Confidence at least 0.80 and parameters valid?}
  unclear[422 NEEDS_INPUT and nothing is called]
  fixed[ai-service calls one fixed catalog or order client]
  record[Save execution audit and outbox together]
  bus[Kafka notification.events.v1]
  inbox[notification-service writes one in-app row]
  mail{aiEmail preference on?}
  smtp[auth-service sends the mail]

  call --> replay
  replay -->|yes| saved
  replay -->|no| tools --> route --> model --> trust
  trust -->|no| unclear
  trust -->|yes| fixed --> record
  record -->|read| stopNode[No notification]
  record -->|write finished| bus --> inbox --> mail
  mail -->|yes| smtp
  mail -->|no| inboxOnly[In-app notification only]
```

ai-inference never receives the shop URL, the caller token, or permission to call another service. `ORDER_CANCEL`, `PAYMENT_REFUND`, and `ROLE_GRANT` stay disabled. A write that times out after it was sent stays `UNKNOWN` until execution history is checked. Reads do not send a notification.

The caller then uses `/api/notifications` to list, count, and mark rows read.

## 7. Audit

```mermaid
flowchart TD
  action[Service writes one audit row in its own database]
  admin[ADMIN or AUDIT_READ calls the gateway]
  path["GET /api/admin/audit-logs/{service}"]
  rewrite[Gateway rewrites the path to /api/admin/audit-logs]
  rows[That service returns its own rows]

  action --> admin --> path --> rewrite --> rows
```

`{service}` is `auth`, `catalog`, `order`, `payment`, `ai`, or `notification`. The audit row stores the action and the request id. It does not store the AI prompt, the model parameters, or the downstream response body.

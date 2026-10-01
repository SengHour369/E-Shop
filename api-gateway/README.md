# E-Shop API Gateway

The gateway follows the gateway in `proctoring-platform` (`D:/spring boot/test`),
adapted to E-Shop's Maven modules, `/api/v1` controllers, and username-based JWTs.
The client entry point is `http://localhost:8080`.

## Structure and request flow

```text
Client -> correlation ID -> client metadata -> completion logger
       -> JWT validation -> Redis rate policy -> Eureka route
       -> downstream service -> response
                              -> circuit breaker -> 503 fallback
```

Under `src/main/java/com/example/eshop/gateway`:

| Folder | Responsibility |
| --- | --- |
| `config` | Typed authentication and logging configuration |
| `filter` | Correlation IDs, trusted headers, JWTs, rate limits, completion logs, OpenAPI rewriting |
| `controller` | Service fallback and dynamic route administration |
| `service` | Fallback response, dynamic route updates, reactive Redis limiter |
| `repository` | Reactive PostgreSQL access |
| `route`, `logging` | Database models and dynamic route loader |
| `dto` | Validated route requests |

The application uses WebFlux, R2DBC and reactive Redis; it does not use servlet
security or blocking JPA in the gateway.

## E-Shop route ownership

All service requests preserve their original path, HTTP method, body and bearer token.
Every business route has its own circuit breaker and `/fallback/{service}` response.
Automatic service-name routes are disabled so only declared routes are exposed.

| Service | Paths below `/api/v1` |
| --- | --- |
| `auth-service` | `auth`, `public`, `user`, `addresses`, `admin/users`, `groups`, `permissions`, `functions`, `user-groups`, `user-permissions`, `group-permissions` |
| `catalog-service` | `products`, `categories`, `category-icons`, `subcategories`, `attributes`, `attribute-values`, `inventory` |
| `order-service` | `cart`, `orders`, `cancelations`, `returns`, `refunds` |
| `payment-service` | `payments`, `payment-transactions`, `bakong` |

Swagger UI is at `/swagger-ui.html`. Its selector loads each service's specification
through `/openapi/{service-name}` and rewrites the server URL to `/`, keeping API
requests on the gateway. The downstream service must be running to load its specification.

## Authentication

Authentication is enabled by default. Login, registration, verification, token refresh,
password recovery and OpenAPI paths are explicitly listed in `application.yml`.
Other routed requests require `Authorization: Bearer <access-token>` or an `eshop_access`
HttpOnly cookie. Browser setup, refresh, logout and CSRF protection are described in
[Cookie authentication](../auth-service/COOKIE_AUTH.md).

Set the same Base64 `JWT_SECRET` for the gateway and auth-service. E-Shop's auth-service
issues HS256 tokens whose subject is a **username**, with an `isEnable` claim.
The gateway validates signature and expiry, requires `isEnable=true`, rejects refresh
tokens, and forwards the subject as `X-Authenticated-Username`. It removes caller-supplied
identity headers and the gateway admin key before forwarding. It never presents a username
as a numeric user ID. Business authorization and account/revocation checks remain the
responsibility of each downstream service; gateway validation alone does not implement them.

`GATEWAY_AUTH_ENABLED=false` is available for local compatibility testing. Identity headers
are still stripped in that mode. Direct service ports in the existing Compose setup bypass
the gateway; restrict those ports when deploying an environment that relies on gateway authentication.

## Start locally

From the repository root:

```powershell
docker compose up -d gateway-db gateway-redis discovery-server
mvn -pl api-gateway -am package
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

Alternatively, build and start the gateway container:

```powershell
docker compose up -d --build api-gateway
```

Start the business services separately to serve their APIs. If a service has no registered
instance, a public request receives a stable HTTP 503 response instead of an internal error.

| Environment variable | Default / purpose |
| --- | --- |
| `GATEWAY_DATABASE_URL` | `r2dbc:postgresql://localhost:5436/gateway_db` |
| `GATEWAY_DATABASE_USERNAME`, `GATEWAY_DATABASE_PASSWORD` | `eshop` for local development |
| `REDIS_HOST`, `REDIS_PORT` | `localhost`, `6379` |
| `JWT_SECRET` | Same existing development fallback as auth-service; replace together for deployment |
| `GATEWAY_AUTH_ENABLED` | `true` |
| `GATEWAY_ADMIN_KEY` | Empty; administration denied until configured |
| `GATEWAY_DYNAMIC_ROUTES_ENABLED` | `true`; enables database route loading and rate policies |
| `GATEWAY_DATABASE_LOGGING_ENABLED` | `true` |
| `GATEWAY_CORS_ORIGINS` | `http://localhost:3000,http://localhost:5173` |

Compose supplies the gateway database and Redis container addresses. `schema.sql` initializes
the gateway's dedicated database. PostgreSQL and Redis must be available for normal operation.
The Java process does not automatically read `.env`; export variables in its shell when running locally.

## Dynamic route administration

All calls to `/api/v1/gateway/routes` require `X-Gateway-Admin-Key` matching the configured
non-empty `GATEWAY_ADMIN_KEY`. This controller uses its own key check independently of the
routed-request JWT filter.

| Method | Endpoint | Action |
| --- | --- | --- |
| GET | `/api/v1/gateway/routes` | List database routes |
| POST | `/api/v1/gateway/routes` | Create route |
| PUT | `/api/v1/gateway/routes/{id}` | Replace route settings |
| DELETE | `/api/v1/gateway/routes/{id}` | Delete route |
| POST | `/api/v1/gateway/routes/refresh` | Refresh local route cache |

Example request body for a separately registered future service:

```json
{
  "routeKey": "recommendation-service",
  "uri": "lb://recommendation-service",
  "pathPattern": "/api/v1/recommendations/**",
  "httpMethod": "GET",
  "enabled": true,
  "rateLimit": 120,
  "rateLimitWindowSeconds": 60
}
```

Only `lb://service-name` destinations are accepted. Use non-overlapping paths for new routes;
the existing static routes retain their ownership. Set both rate fields or neither.
The atomic Redis fixed-window limiter keys requests by authenticated username or socket IP.
Configured database policies can also limit matching static paths. Like the reference,
limiting fails open if Redis or the route database is unavailable; static routes have no
rate limit unless a matching database policy is configured. Route refresh events are local
to one gateway instance; refresh each instance after updates in a multi-instance deployment.

## Logging and failure responses

Safe `X-Correlation-Id` values are preserved; missing or unsafe values are replaced with UUIDs.
Routed requests return and forward that ID. Completion records include method, path, status,
duration and socket IP. They exclude bodies, query strings and authorization headers.
Database logging failures do not replace the client response. Set a retention policy for
`gateway_request_logs` appropriate to the deployment.

Connection timeout is 3 seconds, response timeout 5 seconds, and circuit-breaker time limit
6 seconds. Fallback responses contain `timestamp`, `status: 503`, `service` and `message`.
The gateway exposes only health and info actuator endpoints. Local controllers such as
actuator and route administration do not pass through Gateway GlobalFilters.

## Verification

```powershell
mvn -pl api-gateway -am test
docker compose config --quiet
curl.exe -i http://localhost:8080/api/v1/orders
```

The last request should return 401 without a bearer token. Tests cover correlation IDs,
E-Shop token handling, route ownership, circuit-breaker fallback, CORS preflight, route admin
access, duplicate route keys and the fallback response contract without requiring live services.

# Browser authentication with cookies

The browser uses the gateway on port 8080. `SessionController` handles browser requests,
`AuthServiceImpl` handles credentials and database refresh-token rotation, and
`SessionCookieService` handles cookie creation, deletion and trusted origins.
Existing bearer endpoints in `LoginController` remain available for API clients.

## Processing

1. POST `/api/v1/public/session/login` with the existing `CriteriaValue` and `Password` fields.
2. The response sets `eshop_access` (path `/api`) and `eshop_refresh`
   (path `/api/v1/public/session`). Both are HttpOnly, host-only and SameSite=Lax.
   The JSON response contains account information without token values.
3. Send API requests with `credentials: 'include'`. The gateway validates the access
   cookie locally, removes cookies from protected downstream requests, and forwards
   a bearer header for compatibility with existing service security.
4. On an expired access token, POST `/api/v1/public/session/refresh` without a body.
   The auth service consumes the refresh cookie, rotates its database token and sets
   both cookies again. Coordinate concurrent refreshes in the frontend using one shared promise.
5. POST `/api/v1/public/session/logout` without a body to revoke the refresh token and
   expire both cookies. The endpoint works even after the access cookie has expired.

POST `/api/v1/public/session/verify?email=...&code=...` also creates a cookie session
after email verification. Registration and password recovery keep their existing routes.

```javascript
const gateway = 'http://localhost:8080';
await fetch(`${gateway}/api/v1/public/session/login`, {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ CriteriaValue: 'your-username', Password: 'your-password' })
});

await fetch(`${gateway}/api/v1/orders`, { credentials: 'include' });
await fetch(`${gateway}/api/v1/public/session/refresh`, {
  method: 'POST', credentials: 'include'
});
await fetch(`${gateway}/api/v1/public/session/logout`, {
  method: 'POST', credentials: 'include'
});
```

Use the actual order endpoint required by the existing controller. JavaScript cannot
read the token cookies and should not copy tokens into localStorage.

## Settings and security

- `AUTH_COOKIE_SECURE=true` is the default for HTTPS. Set it to `false` explicitly for local HTTP.
- `GATEWAY_CORS_ORIGINS` must list exact trusted frontend origins in both services,
  for example `http://localhost:5173,http://localhost:3000`. Do not use `*`.
- Every session POST and every unsafe cookie-authenticated gateway request requires
  a matching `Origin` header. Browsers set this automatically; this check prevents CSRF,
  including login CSRF. Non-browser tests must supply it explicitly.
- SameSite=Lax requires the frontend and API to be same-site. For deployment, put them
  under the same site or proxy API requests through the frontend's site.
- `JWT_EXPIRATION` now defaults to 900 seconds (15 minutes). Refresh tokens retain the
  existing 7-day database lifetime. Use the same `JWT_SECRET` in auth and gateway.
- An explicit Authorization header takes precedence over the cookie, and an invalid
  header never falls back to cookies. Bearer-only requests remain compatible.
- Logout revokes refresh and clears browser cookies; a previously copied access JWT
  remains valid until expiry. Gateway validation is stateless and does not implement revocation.

## Request-path efficiency

The gateway constructs its thread-safe JWT parser once, with no auth-service call per
request. Rate-limit policies and compiled path patterns share a 30-second reactive cache,
invalidated immediately by route refresh events on that gateway instance. Redis still
counts requests atomically; its result is not cached. Database policy loads and Redis
operations have one-second time limits and retain the reference's fail-open behavior.
Auth-service retains its existing account checks. No throughput or latency benchmark
has been claimed for these changes.

Run checks from the project root:

```powershell
mvn -pl api-gateway,auth-service -am test
```

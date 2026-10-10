# Run E-Shop locally

Start the local HTTP stack:

```powershell
docker compose -f compose.yaml -f compose.local.yaml up -d --build
```

- Gateway and Swagger: http://localhost:8080/swagger-ui.html
- Service discovery: http://localhost:8761
- Local email inbox (verification codes and password resets): http://localhost:8025
- Auth, Catalog, Order, Payment: ports 8081, 8082, 8083, 8084.

The local override enables cookies over HTTP. For HTTPS deployments, omit
compose.local.yaml so cookies retain their secure default.

The local override sends all email to Mailpit, without contacting real recipients.
Open the local inbox to read the verification code after registering. This avoids
Gmail authentication errors when SMTP credentials are not configured. Real email
delivery requires valid SMTP settings in the deployed auth-service environment.

If registration reports `Authentication failed`, check the SMTP configuration:
the default Gmail password is a placeholder. Use the local command above to
route verification emails to Mailpit. Email transport failures return HTTP 503
with `EMAIL_DELIVERY_FAILED`; they do not mean the new user's password is wrong.

Check status or logs:

```powershell
docker compose -f compose.yaml -f compose.local.yaml ps
docker compose -f compose.yaml -f compose.local.yaml logs --tail=100 catalog-service
```

Stop containers while retaining database volumes:

```powershell
docker compose -f compose.yaml -f compose.local.yaml down
```

PostgreSQL 18 data is persisted at /var/lib/postgresql in named volumes.
Service builds share a Maven dependency cache. Email, image uploads, and live
payments still require their respective external-service credentials.

Auth, catalog, order, payment, the API gateway, ai-service, and notification-service
share the `gateway-redis` container (`redis:7-alpine` on `127.0.0.1:6379`).
Compose sets `REDIS_HOST=gateway-redis`. A local run outside Compose uses
`localhost:6379`. Each service records `{service-name}:redis` for two minutes
when it starts. If Redis is down, the service still starts and the gateway
rate limiter allows the request. Discovery and ai-inference do not use Redis.
Stock and promotion prices are not cached.

# E-Shop database configuration

This guide describes the repository configuration checked on 2026-10-10.
It covers all eight PostgreSQL databases, Redis, database ownership, connection
settings, schema scripts, and local development ports. It is not a live schema
dump. Passwords and tokens are intentionally not copied into this document.

## All PostgreSQL databases

The Compose configuration uses PostgreSQL 18. Every database container listens
on port `5432` internally. Each has a different published port on your computer.

| Database | Owning service | Docker database hostname | Port on your computer | Named storage volume |
| --- | --- | --- | --- | --- |
| `auth_db` | `auth-service` | `auth-db` | `5432` | `auth_db_data` |
| `catalog_db` | `catalog-service` | `catalog-db` | `5433` | `catalog_db_data` |
| `order_db` | `order-service` | `order-db` | `5434` | `order_db_data` |
| `payment_db` | `payment-service` | `payment-db` | `5435` | `payment_db_data` |
| `gateway_db` | `api-gateway` | `gateway-db` | `5436` | `gateway_db_data` |
| `ai_db` | `ai-service` | `ai-db` | `5437` | `ai_db_data` |
| `notification_db` | `notification-service` | `notification-db` | `5438` | `notification_db_data` |
| `admin_db` | `admin-service` | `admin-db` | `5439` | `admin_db_data` |

Docker may prefix the actual volume name with the Compose project name.
Database data is mounted at `/var/lib/postgresql` inside each container.

The base databases are defined in [compose.yaml](../compose.yaml).
AI and notification databases are added by [compose.ai.yaml](../compose.ai.yaml).

## Connect from your computer

Use these settings in pgAdmin, DBeaver, an IDE, or a service running outside
Docker. Use the configured database username and password from your local
configuration or Compose environment.

| Database | Host | JDBC URL |
| --- | --- | --- |
| `auth_db` | `localhost` | `jdbc:postgresql://localhost:5432/auth_db` |
| `catalog_db` | `localhost` | `jdbc:postgresql://localhost:5433/catalog_db` |
| `order_db` | `localhost` | `jdbc:postgresql://localhost:5434/order_db` |
| `payment_db` | `localhost` | `jdbc:postgresql://localhost:5435/payment_db` |
| `gateway_db` | `localhost` | `jdbc:postgresql://localhost:5436/gateway_db` |
| `ai_db` | `localhost` | `jdbc:postgresql://localhost:5437/ai_db` |
| `notification_db` | `localhost` | `jdbc:postgresql://localhost:5438/notification_db` |
| `admin_db` | `localhost` | `jdbc:postgresql://localhost:5439/admin_db` |

The gateway application uses R2DBC rather than JDBC. Its application URL is
`r2dbc:postgresql://localhost:5436/gateway_db`. A database GUI can still use
the normal PostgreSQL connection shown above.

### Current local port mismatch

The catalog, order, and payment `application.yml` files currently default to
`localhost:5432`. That is suitable only if those databases exist on a separate
local PostgreSQL server at that port. When using the project Docker databases,
override those URLs with ports `5433`, `5434`, and `5435` respectively.

For example, when launching catalog-service from your computer:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/catalog_db'
```

This guide does not modify any application settings or running databases.

## Connect between Docker services

Services on the Compose network use database service names and internal port
`5432`. They do not use the published host ports.

```text
auth-service         -> jdbc:postgresql://auth-db:5432/auth_db
catalog-service      -> jdbc:postgresql://catalog-db:5432/catalog_db
order-service        -> jdbc:postgresql://order-db:5432/order_db
payment-service      -> jdbc:postgresql://payment-db:5432/payment_db
api-gateway          -> r2dbc:postgresql://gateway-db:5432/gateway_db
ai-service           -> jdbc:postgresql://ai-db:5432/ai_db
notification-service -> jdbc:postgresql://notification-db:5432/notification_db
admin-service        -> jdbc:postgresql://admin-db:5432/admin_db
```

## Configuration files and environment variables

| Service | Application configuration | Connection environment variables used by Compose |
| --- | --- | --- |
| Auth | [application.yml](../auth-service/src/main/resources/application.yml) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Catalog | [application.yml](../catalog-service/src/main/resources/application.yml) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Order | [application.yml](../order-service/src/main/resources/application.yml) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Payment | [application.yml](../payment-service/src/main/resources/application.yml) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Gateway | [application.yml](../api-gateway/src/main/resources/application.yml) | `GATEWAY_DATABASE_URL`, `GATEWAY_DATABASE_USERNAME`, `GATEWAY_DATABASE_PASSWORD` |
| AI | [application.yml](../ai-service/src/main/resources/application.yml) | `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` |
| Notification | [application.yml](../notification-service/src/main/resources/application.yml) | `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` |
| Admin | [application.yml](../admin-service/src/main/resources/application.yml) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |

Admin's application file also supports `DATABASE_URL`, `DATABASE_USERNAME`,
and `DATABASE_PASSWORD`; Compose supplies the standard Spring overrides.
Compose maps `AI_DATABASE_USERNAME` and `AI_DATABASE_PASSWORD` to the AI
database and service. Notification uses the corresponding
`NOTIFICATION_DATABASE_USERNAME` and `NOTIFICATION_DATABASE_PASSWORD` settings.

Spring environment variables override the application-file defaults.
Compose's root `.env` supplies `${...}` substitutions declared in its YAML;
Spring Boot does not automatically load arbitrary `.env` files itself.
Auth, catalog, order, payment, and admin currently also contain literal
development credentials in their Compose definitions. Changing an unrelated
`.env` variable will not replace those literals.

## Data owned by each database

These names come from entity mappings and SQL files. Optional tables require
their migration, and a running older deployment may have a different schema.

| Database | Data and mapped tables |
| --- | --- |
| `auth_db` | Accounts: `tbl_user`, `tbl_role`, `users_roles`, `addresses`, `user_address`. Authorization: `tt_permission`, `tt_group`, `function_permissions`, `api_permissions`, `user_permissions`, `user_groups`, `group_permissions`. Tokens: `verification_tokens`, `password_reset_tokens`, `refresh_tokens`. Mail delivery: `notification_mail_deliveries`. |
| `catalog_db` | Products: `products`, `product_skus`, `categories`, `sub_categories`, `images`, `category_icons`, `brand_logos`. Variants: `attributes`, `attribute_values`, `variant_attributes`. Inventory: `inventories`, `stock_movements`, `checkout_reservations`. Promotions: `promotions`, `promotion_skus`, `promotion_usages`. |
| `order_db` | `carts`, `cart_items`, `order_details`, `order_items`, `tbl_order_cancelation`, `tbl_return_request`, `return_status_history`, `refunds`, `refund_status_history`. |
| `payment_db` | `payments`, `payment_transactions`, `payment_transaction_status_history`. |
| `gateway_db` | `gateway_routes`, `gateway_request_logs`, `gateway_admin_keys`. |
| `ai_db` | `ai_executions`, `notification_outbox`, optional `ai_knowledge_chunks`. |
| `notification_db` | `notifications`, `notification_preferences`. |
| `admin_db` | `store_settings`, `admin_service_ticks`. |

JPA services also scan the shared `audit_logs` entity into their own database.
This is separate local audit storage, not one database shared by every service.
Cross-service records use identifiers and service calls; for example, payment
stores an order ID instead of a cross-database JPA relationship to an order.

### Product relationships

```text
catalog_db
  categories -> sub_categories -> products -> product_skus -> inventories
                                             |
                                             +-> variant_attributes
                                             +-> promotion_skus
```

Product pricing, SKU availability, stock movements, and promotions remain owned
by catalog-service. AI retrieves current product data through that service.

## Schema setup and SQL files

JPA services currently use Hibernate `ddl-auto: update` by default. AI,
notification, and admin also expose `DDL_AUTO` in their application settings.
The gateway runs [schema.sql](../api-gateway/src/main/resources/schema.sql)
through Spring SQL initialization with `mode: always`.

Additional schema changes are stored in each service's
`src/main/resources/db/upgrade` directory. The repository does not configure
Flyway or Liquibase to automatically run those upgrade files. Apply each script
to its owning database through the project's migration procedure.

AI knowledge retrieval additionally requires pgvector and the
[knowledge migration](../ai-service/src/main/resources/db/upgrade/20261008_ai_knowledge.sql).
The stock PostgreSQL image does not include the pgvector binaries. The feature
is disabled by default and must be configured separately.

Seed SQL files are available in [db/seed](../db/seed), covering auth, catalog,
order, payment, AI, notification, and gateway. They are not automatically
mounted as initialization scripts by the Compose files. No seed or migration
was executed when creating this document.

## Redis and other infrastructure

| Component | Host connection | Docker connection | Purpose |
| --- | --- | --- | --- |
| Redis | `localhost:6379` | `gateway-redis:6379` | Service caches, gateway limits, and AI rate limits, conversation references, and expiring confirmations |
| Kafka | `localhost:9092` | `kafka:19092` | Events and notification delivery; not a relational database |

Redis uses `redis:7-alpine`. The current Compose configuration defines no Redis
data volume. Kafka stores broker data in the `kafka_data` volume.

Discovery-server and the Python ai-inference process do not have dedicated
PostgreSQL databases configured. `common-lib` is a shared library, not a
separate database service. Selected automated tests use in-memory H2 databases;
those are separate from the eight PostgreSQL databases above.

# API Testing Guide (Postman-style)

Copy-paste-ready request/response examples for the dynamic permission system
(`ApiPermission` → `FunctionPermission` → `UserPermission`), the product
soft-delete fix, and the Order Cancelations reporting module. Each block
mirrors a Postman request: **Method + URL**, **Headers**, **Body**,
**Example Response**.

Base URL for all examples: `http://localhost:8080`

---

## 0. Variables (set these up as a Postman Environment)

| Variable      | Example value                          |
|---------------|-----------------------------------------|
| `base_url`    | `http://localhost:8080`                 |
| `token`       | *(filled in after step 1)*              |

---

## 1. Auth — Login

**POST** `{{base_url}}/authorization`

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "CriteriaValue": "admin",
  "Password": "admin123"
}
```

**Example Response — 200:**
```json
{
  "id": 1,
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer",
  "email": "admin@example.com",
  "username": "admin",
  "role": "ADMIN",
  "message": "Login successful"
}
```

Save `access_token` as `{{token}}`. Every request below sends:
```
Authorization: Bearer {{token}}
```

---

## 2. Function Permissions (`/api/v1/functions`)

Uses `@RequestParam`, not JSON body — send as `x-www-form-urlencoded` or query
params in Postman.

### 2.1 Create
**POST** `{{base_url}}/api/v1/functions/create/`
**Headers:** `Authorization: Bearer {{token}}`
**Body (x-www-form-urlencoded):**
```
funcCode=PRODUCT_DELETE
funcName=Delete Product
description=Soft delete a product
module=PRODUCT
```
**Example Response — 201:**
```json
{
  "success": true,
  "message": "Function created successfully",
  "data": {
    "funcId": 404,
    "funcCode": "PRODUCT_DELETE",
    "funcName": "Delete Product",
    "description": "Soft delete a product",
    "module": "PRODUCT",
    "isActive": true
  }
}
```
`funcId` is `max(existing funcId) + 1` — with the seeded set (101–403) the
first custom function created lands at `404`.

### 2.2 Get all / by id
**POST** `{{base_url}}/api/v1/functions/get/all`
**Body:**
```json
{ "criteria_type": 0, "page": 1, "size": 10 }
```

**POST** `{{base_url}}/api/v1/functions/get/id/?id=101`

### 2.3 Update
**POST** `{{base_url}}/api/v1/functions/update/`
**Body (x-www-form-urlencoded):**
```
id=404
funcName=Delete Product (soft)
isActive=true
```

### 2.4 Delete
**POST** `{{base_url}}/api/v1/functions/delete/`
**Body (x-www-form-urlencoded):**
```
id=404
```

---

## 3. Group Permissions (`/api/v1/group-permissions`)

### 3.1 Create — grant a `funcId` to a group
**POST** `{{base_url}}/api/v1/group-permissions/create/`
**Body (x-www-form-urlencoded):**
```
groupId=1
funcId=101
```

### 3.2 Get all
**POST** `{{base_url}}/api/v1/group-permissions/get/all`
**Body:**
```json
{ "criteria_type": 0, "page": 1, "size": 10 }
```

### 3.3 Update / Delete
**POST** `{{base_url}}/api/v1/group-permissions/update/`
```
id=1
isActive=false
```
**POST** `{{base_url}}/api/v1/group-permissions/delete/`
```
id=1
```

---

## 4. User Groups (`/api/v1/user-groups`)

### 4.1 Create
**POST** `{{base_url}}/api/v1/user-groups/create/`
**Body (x-www-form-urlencoded):**
```
groupCode=QA
groupName=QA Team
display=Quality Assurance
```

### 4.2 Get all / Update / Delete
Same shape as Function Permissions above — `get/all` takes `criteria_type` /
`criteria_value` / `page` / `size`; `update`/`delete` take `id` (+ fields to
change).

---

## 5. User Permissions (`/api/v1/user-permissions`) — the actual access grant

### 5.1 Create — grant a `funcId` directly to a user
**POST** `{{base_url}}/api/v1/user-permissions/create/`
**Body (x-www-form-urlencoded):**
```
userId=1
funcId=102
```
**Example Response — 201:**
```json
{
  "success": true,
  "message": "User permission created successfully",
  "data": { "userPermissionId": 5, "userId": 1, "funcId": 102, "isActive": true }
}
```

### 5.2 Update (enable/disable) / Delete
```
POST /api/v1/user-permissions/update/   body: id=5&isActive=false
POST /api/v1/user-permissions/delete/   body: id=5
```

---

## 6. Wiring a new endpoint into `api_permissions`

There's no CRUD controller for this table yet — insert directly:

```sql
INSERT INTO api_permissions (method, api, func_id, is_active, is_delete, created_at, updated_at)
VALUES ('POST', '/api/v1/user-groups/create/', 102, true, false, now(), now());
```

Path patterns support Ant-style wildcards: `*` = one segment, `**` = many.
Example: `/admin/returns/*/approve` matches `/admin/returns/123/approve`.

---

## 7. End-to-end permission flow (the important test)

This is the sequence that proves the dynamic check actually works — run in order.
**Use a non-admin user's token, not `{{token}}` from §1** — the seeded `admin`
account bypasses every check below (§11), so all three steps would just return
201 immediately and prove nothing. Log in as a different user first and use
that token instead.

**Step 1 — hit a protected route before anything is configured:**
**POST** `{{base_url}}/api/v1/user-groups/create/`
**Headers:** `Authorization: Bearer {{token}}`
**Body:** `groupCode=TEST&groupName=Test Group`

**Response — 403 (no `api_permissions` row yet):**
```json
{
  "status": 403,
  "error": "Forbidden",
  "errorCode": "FORBIDDEN",
  "message": "API not configured for permission check: POST /api/v1/user-groups/create/"
}
```

**Step 2 — add the mapping** (SQL from §6, `funcId=102`), then retry:

**Response — 403 (mapping exists, but user has no grant):**
```json
{
  "status": 403,
  "error": "Forbidden",
  "errorCode": "FORBIDDEN",
  "message": "You do not have permission to perform this action"
}
```

**Step 3 — grant it** (§5.1: `userId=1&funcId=102`), then retry the same request:

**Response — 201:**
```json
{
  "success": true,
  "message": "User group created successfully",
  "data": { "id": 5, "groupCode": "TEST", "groupName": "Test Group", "isActive": true }
}
```

---

## 8. Products

### 8.1 Create — multipart/form-data (not JSON)

**POST** `{{base_url}}/api/v1/products/create/`
**Headers:** `Authorization: Bearer {{token}}` (Content-Type is set automatically by Postman for form-data)

**Body → form-data:**

| Key              | Type | Value                                              |
|------------------|------|-----------------------------------------------------|
| `name`           | Text | `Wireless Mouse`                                    |
| `description`    | Text | `Ergonomic 2.4GHz wireless mouse`                    |
| `is_active`      | Text | `true`                                               |
| `sub_category_id`| Text | `3`                                                  |
| `skus`           | Text | `[{"price":19.99,"quantity":100,"isDefault":true}]` |
| `files`          | File | *(pick 1+ image files)*                              |
| `sku_images`     | File | *(optional, 1 per SKU)*                              |

`skus` is a JSON-stringified array of `ProductSkuRequest`:
```json
[
  {
    "price": 19.99,
    "quantity": 100,
    "lowStockThreshold": 5,
    "isDefault": true,
    "inventory": { "quantity": 100, "warehouseLocation": "WH-A1" }
  }
]
```

**Example Response — 201:**
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": { "id": 42, "name": "Wireless Mouse", "isActive": true, "deleted": false }
}
```

### 8.2 Get products (all / by id / search / by category / active-only)

**POST** `{{base_url}}/api/v1/products/get/all`

| `criteria_type` | Meaning                | `criteria_value` example |
|------------------|-------------------------|---------------------------|
| `0` or omitted   | all products            | —                          |
| `1`              | search by name/desc     | `mouse`                    |
| `2`              | by subcategory id       | `3`                         |
| `3`              | by category id          | `1`                         |
| `4`              | active only             | —                          |
| `5`              | single product by id    | `42`                        |
| `6`              | single product + SKUs   | `42`                        |

**Body:**
```json
{ "criteria_type": 5, "criteria_value": "42", "page": 1, "size": 10 }
```

### 8.3 Delete — now a soft delete

**POST** `{{base_url}}/api/v1/products/delete/?id=42`
**Headers:** `Authorization: Bearer {{token}}` (must have `ADMIN` authority)

**Example Response — 200:**
```json
{ "success": true, "message": "Product deleted successfully", "data": null }
```

**Verify it's soft, not hard:**
- Row still exists in Postgres with `deleted = true`.
- `POST /api/v1/products/get/all` with `{"criteria_type": 5, "criteria_value": "42"}` → **404**:
  ```json
  { "status": 404, "error": "Not Found", "message": "Product not found with id: 42" }
  ```
- `POST /api/v1/products/get/all` with `{"page": 1, "size": 50}` (no filter) → product `42` is
  simply absent from `data.payload`, everything else still lists normally.

---

## 9. Order / Payment history (Postgres `$N` type-inference fix)

Confirms the `CAST(:param AS ...)` fix for optional filters didn't break anything.

**POST** `{{base_url}}/api/v1/orders/user/history`
**Headers:** `Authorization: Bearer {{token}}`
**Body — all filters null (this used to 500):**
```json
{ "status": null, "startDate": null, "endDate": null, "page": 1, "size": 10 }
```
**Example Response — 200:**
```json
{
  "success": true,
  "message": "Orders retrieved successfully",
  "data": { "payload": [ /* ... */ ], "totalItems": 3, "currentPage": 1 }
}
```

**Body — with a date filter set:**
```json
{ "status": "DELIVERED", "startDate": "2026-01-01T00:00:00", "endDate": "2026-07-01T00:00:00", "page": 1, "size": 10 }
```

Same request shape applies to **POST** `{{base_url}}/api/v1/payments/user/history`.

---

## 10. Order Cancelations (`/admin/cancelations`)

Read-only admin reporting over `tbl_order_cancelation`. Records are created
automatically whenever a customer cancels a pending order via
`POST /api/v1/orders/user/cancel` — there's no create endpoint on this
controller itself.

**Not wired into `api_permissions` yet** — for a *non-admin* user, every endpoint
below will 403 with `"API not configured for permission check"` until you add
`function_permissions` + `api_permissions` rows for them (see §6). No `funcId`
range has been assigned yet for this module. The seeded `admin` account (or any
user with the `ADMIN` authority) bypasses this entirely — see §11 below — so
testing with `{{token}}` from §1 (the admin login) will succeed even without
those rows.

### 10.1 Trigger a cancelation record

**POST** `{{base_url}}/api/v1/orders/user/cancel?id=42&userId=1`
**Headers:** `Authorization: Bearer {{token}}`

Order `42` must belong to user `1` and be in `PENDING` status. This both
cancels the order and inserts a row into `tbl_order_cancelation`
(`cancelReason=CUSTOMER_REQUESTED`, `cancelStatus=CANCELED`).

**Example Response — 200:**
```json
{
  "success": true,
  "message": "Order retrieved successfully",
  "data": { "id": 42, "orderNumber": "ORD-A1B2C3D4", "status": "CANCELLED" }
}
```

### 10.2 Summary

**GET** `{{base_url}}/admin/cancelations/summary`
**Headers:** `Authorization: Bearer {{token}}`

**Example Response — 200:**
```json
{
  "success": true,
  "message": "Cancelation summary retrieved successfully",
  "data": {
    "totalCancelations": 12,
    "pendingReview": 0,
    "cancelationRate": 3.4,
    "valueLost": 458.50
  }
}
```
`pendingReview` counts `REQUESTED`/`PENDING_REVIEW` records — customer
self-cancels land straight in `CANCELED`, so this stays `0` until an
admin-initiated or fraud-flagged flow (not built yet) creates a record in
one of those states.

### 10.3 List (search / filter)

**POST** `{{base_url}}/admin/cancelations/list`
**Headers:** `Authorization: Bearer {{token}}`

**Body — no filters:**
```json
{ "page": 1, "size": 10 }
```

**Body — filtered:**
```json
{
  "page": 1,
  "size": 10,
  "orderNo": "ORD-A1B2C3D4",
  "customerName": "admin",
  "cancelReason": "CUSTOMER_REQUESTED",
  "cancelStatus": "CANCELED",
  "fromCancelDate": "2026-01-01T00:00:00",
  "toCancelDate": "2026-12-31T23:59:59",
  "minAmount": 10,
  "maxAmount": 1000
}
```

**Example Response — 200:**
```json
{
  "success": true,
  "message": "Cancelations retrieved successfully",
  "data": {
    "payload": [
      {
        "orderNo": "ORD-A1B2C3D4",
        "customerName": "System Administrator",
        "cancelDate": "2026-07-09T11:20:00",
        "cancelReason": "CUSTOMER_REQUESTED",
        "amount": 89.99
      }
    ],
    "total_items": 1,
    "total_pages": 1,
    "current_page": 1,
    "page_size": 10
  }
}
```

Validation: `fromCancelDate` > `toCancelDate` or `minAmount` > `maxAmount` → 400.

### 10.4 Detail — click-through from the list's `orderNo`

**GET** `{{base_url}}/admin/cancelations/ORD-A1B2C3D4`
**Headers:** `Authorization: Bearer {{token}}`

**Example Response — 200:**
```json
{
  "success": true,
  "message": "Cancelation detail retrieved successfully",
  "data": {
    "cancelationId": null,
    "orderId": 42,
    "orderNo": "ORD-A1B2C3D4",
    "customerId": 1,
    "customerName": "System Administrator",
    "cancelReason": "CUSTOMER_REQUESTED",
    "cancelStatus": "CANCELED",
    "cancelSource": "CUSTOMER",
    "cancelDate": "2026-07-09T11:20:00",
    "amount": 89.99,
    "currency": "USD",
    "remark": "Cancelled by customer",
    "reviewedAt": null,
    "reviewedBy": null,
    "createdAt": "2026-07-09T11:20:00",
    "createdBy": "admin",
    "updatedAt": "2026-07-09T11:20:00",
    "updatedBy": null
  }
}
```

**Unknown order number — 404:**
```json
{ "status": 404, "error": "Not Found", "errorCode": "CANCELATION_NOT_FOUND", "message": "Cancelation record not found for order: ORD-DOES-NOT-EXIST" }
```

---

## 11. Super admin bypass

Any user whose JWT carries the `ADMIN` authority — the seeded `admin` account, or
anyone granted the `ADMIN` role — skips the entire `ApiPermissionInterceptor` check.
No `api_permissions` row, no `user_permissions` grant needed; it works even on
endpoints that were never wired up (Return, Cancelation).

This means `{{token}}` from §1 (admin login) will get **200/201 on every endpoint
in this document**, including ones §6/§7/§10 describe as 403-until-configured.
Those 403 examples are only reproducible with a non-admin user's token — log in
as a different account to actually see the fail-closed behavior in action.

---

## Quick reference: expected status codes

| Scenario                                                     | Status |
|----------------------------------------------------------------|--------|
| Public route (`/api/v1/products/active`, `/api/v1/auth/**`, ...)| 200, no auth needed |
| No `Authorization` header on a protected route                 | 401 |
| Caller has the `ADMIN` authority (§11)                          | 200/201 always — every row below is skipped |
| Non-admin, protected route, no `api_permissions` row for it     | 403 `API not configured...` |
| Non-admin, row exists, user has no `user_permissions` grant     | 403 `You do not have permission...` |
| Non-admin, row exists, user has an active grant                 | 200/201 |
| Deleted product fetched by id                                   | 404 |
| Deleted product in a list/search query                          | absent from results, no error |
| Non-admin on `/admin/cancelations/**` (no rows exist for it yet) | 403 `API not configured...` (see §10) |
| Cancelation detail for an unknown `orderNo`                     | 404 `CANCELATION_NOT_FOUND` |



Method	Path	Purpose
GET	/	List all orders (paged)
POST	/get/all	List orders by criteria (GetOrderRequest)
POST	/user/id/	Get a user's orders (paged)
POST	/id/	Get order by id
POST	/number/	Get order by order number
POST	/user/detail	Get order detail for a specific user
POST	/user/history	Order history for a user (status/date filters)
POST	/user/from-cart	Create order from cart — the generic entry point; auto-pushes to KHQR (Bakong/ABA/ACLEDA) if that method is selected
POST	/status/	Update order status
POST	/user/cancel	Cancel an order
POST	/user/from-cart/bakong	Bakong-only variant — now just delegates to the same logic as /user/from-cart
POST	/bakong/initiate	(Re-)generate KHQR code + deep link for a pending order
POST	/bakong/verify	Verify payment via MD5 against Bakong
POST	/bakong/callback	Webhook-style callback to confirm payment
GET	/summary	Order status summary
POST	/items	Get order items by order id
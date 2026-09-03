# E-Shop Complete Implementation Guide

This document consolidates the three main modules documented in Email.md, OPTIONAL_ATTRIBUTES_GUIDE.md, and SERVICE_ORDER_TASKS.md

---

## Table of Contents

1. [Auth Module (Email.md)](#auth-module)
2. [Returns Management Module (SERVICE_ORDER_TASKS.md)](#returns-management)
3. [API Permissions System (OPTIONAL_ATTRIBUTES_GUIDE.md)](#api-permissions)
4. [Quick Setup & Testing](#quick-setup)

---

## Auth Module

### Overview
Complete authentication system with:
- User registration with email verification
- JWT access token + refresh token
- Password reset via email
- Token rotation on refresh
- Email verification tokens (24h expiration)
- Password reset tokens (1h expiration)

### Key Entities

#### User (auth/entity/User.java)
- UUID id (generated)
- String username (unique)
- String email (unique) - used as JWT subject
- String password (BCrypt encoded)
- boolean enabled (false until email verified)
- Enum role (USER, ADMIN)
- Timestamps (created_at, updated_at)

#### VerificationToken
- Token expires after 24 hours
- One-to-one with User (deleted after use)

#### RefreshToken
- Token expires after configured duration (7 days default)
- Many-to-one with User (all deleted on password reset)

#### PasswordResetToken
- Token expires after 1 hour
- Many-to-one with User
- Has `used` flag (one-time use)

### API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| GET | `/api/auth/verify?token=xxx` | No | Verify email |
| POST | `/api/auth/resend?email=xxx` | No | Resend verification email |
| POST | `/api/auth/login/email` | No | Login with email + password |
| POST | `/api/auth/login/username` | No | Login with username + password |
| POST | `/api/auth/refresh` | No | Refresh access token |
| POST | `/api/auth/logout` | No | Logout (delete refresh token) |
| POST | `/api/auth/forgot-password` | No | Request password reset |
| POST | `/api/auth/reset-password` | No | Reset password with token |

### Key Features

✅ Email verification required before login
✅ Password reset via email with 1-hour expiring token
✅ Refresh token rotation on each use
✅ Auto-logout all sessions on password reset
✅ Async email sending
✅ Email enumeration prevention on forgot-password
✅ BCrypt password encoding

---

## Returns Management Module

### Overview
Complete returns/refund/exchange management system for admin:
- Customer can request return/refund/exchange
- Admin reviews and approves/rejects
- Full audit trail with status history
- Dashboard summary
- Advanced filtering & search

### Key Entities

#### Return (tbl_return_request)
- Long id
- String returnId (unique, auto-generated: RET-XXXXXXXX)
- Long orderId, customerId, productId
- String returnType (RETURN, REFUND, EXCHANGE)
- String status (REQUESTED, APPROVED, REJECTED, COMPLETED, RECEIVED, INSPECTING, INSPECTED)
- BigDecimal amount
- Timestamps for each stage (requested_at, approved_at, rejected_at, received_at, inspected_at, completed_at)
- Actor tracking (requested_by, approved_by, rejected_by, received_by, inspected_by, completed_by)
- String remark (notes)
- Audit fields (created_by, updated_by, created_at, updated_at)

#### ReturnStatusHistory
- Tracks all status transitions
- Stores old_status → new_status
- Timestamp & actor of each change

### API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/admin/returns/user` | Yes | Customer creates return request |
| GET | `/admin/returns/summary` | Yes | Dashboard summary cards |
| POST | `/admin/returns/list` | Yes | List returns with filters |
| GET | `/admin/returns/{returnId}` | Yes | Get return detail |
| GET | `/admin/returns/{returnId}/history` | Yes | Get status history |
| POST | `/admin/returns/{returnId}/approve` | Yes | Approve return |
| POST | `/admin/returns/{returnId}/reject` | Yes | Reject return |
| POST | `/admin/returns/{returnId}/receive` | Yes | Receive returned item |
| POST | `/admin/returns/{returnId}/inspect/start` | Yes | Start inspection |
| POST | `/admin/returns/{returnId}/inspect/complete` | Yes | Complete inspection |

### Return Status Flow

```
REQUESTED
├─→ APPROVED ────→ RECEIVED ────→ INSPECTING ────→ INSPECTED ────→ COMPLETED
└─→ REJECTED (final)
```

### Key Features

✅ Unique return ID generation
✅ Multi-stage workflow (request → approve/reject → receive → inspect → complete)
✅ Automatic refund integration on approval
✅ Status history tracking
✅ Advanced search (by returnId, orderNo, customerName, productName, returnType, status, date range)
✅ Dashboard summary (total, processed, pending, return rate)
✅ Pagination & sorting

---

## API Permissions System

### Overview
Dynamic permission checking system that:
- Maps HTTP endpoints to function IDs
- Assigns function permissions to user groups
- Assigns function permissions directly to users
- Supports wildcards in URL patterns (*, **)
- Admin users bypass all checks

### Key Entities

#### ApiPermission (api_permissions table)
- String method (POST, GET, PUT, DELETE, etc.)
- String api (URL pattern: `/api/v1/users/create/`, `/admin/*/approve`, etc.)
- Long funcId (function ID)
- boolean isActive
- Supports Ant-style wildcards: `*` = one segment, `**` = many segments

#### FunctionPermission
- Long funcId (unique, auto-increment)
- String funcCode (e.g., PRODUCT_DELETE)
- String funcName (display name)
- String module (PRODUCT, USER, ORDER, etc.)
- boolean isActive

#### GroupPermission
- Links Group → FunctionPermission
- Many groups can have same function
- One function can be in many groups

#### UserPermission
- Links User → FunctionPermission directly
- Direct user grants take precedence

#### UserGroup
- User can be in multiple groups
- Each group has set of permissions (via GroupPermission)

### Check Flow

```
1. Is it a public endpoint? → ALLOW
2. Is user authenticated? → NO → 401 Unauthorized
3. Does user have ADMIN role? → YES → ALLOW (bypass all checks)
4. Is there an api_permissions row for this endpoint? → NO → 403 API not configured
5. Does user have this funcId (direct or via group)? → NO → 403 Forbidden
6. Is the grant active (isActive=true)? → NO → 403 Forbidden
7. Allow → 200/201 OK
```

### API Endpoints (Permission Management)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/functions/create/` | Create function permission |
| POST | `/api/v1/functions/get/all` | List all functions |
| POST | `/api/v1/functions/get/id/?id=101` | Get function by id |
| POST | `/api/v1/functions/update/` | Update function |
| POST | `/api/v1/functions/delete/` | Delete function |
| POST | `/api/v1/group-permissions/create/` | Assign function to group |
| POST | `/api/v1/group-permissions/get/all` | List group permissions |
| POST | `/api/v1/group-permissions/update/` | Update group permission |
| POST | `/api/v1/group-permissions/delete/` | Delete group permission |
| POST | `/api/v1/user-groups/create/` | Create user group |
| POST | `/api/v1/user-groups/get/all` | List user groups |
| POST | `/api/v1/user-groups/update/` | Update user group |
| POST | `/api/v1/user-groups/delete/` | Delete user group |
| POST | `/api/v1/user-permissions/create/` | Grant function directly to user |
| POST | `/api/v1/user-permissions/get/all` | List user permissions |
| POST | `/api/v1/user-permissions/update/` | Update user permission |
| POST | `/api/v1/user-permissions/delete/` | Delete user permission |

### Seeded Functions (funcId 101-403)

Functions are pre-seeded in the database by funcId ranges:
- 101-200: Product management
- 201-300: Order management
- 301-400: User/Admin management
- 404+: Custom functions

First custom function gets id 404.

### Key Features

✅ Wildcard URL pattern matching
✅ Role-based access control (via groups)
✅ User-level permission grants
✅ Admin bypass
✅ Dynamic configuration (no code changes needed)
✅ Active/inactive toggle per permission

---

## Order Cancelations Module

### Overview
Read-only admin reporting on order cancellations:
- Auto-triggered when customer cancels pending order
- Tracks cancellation reason & value
- Admin dashboard & detailed reports

### Key Entities

#### OrderCancelation (tbl_order_cancelation)
- String orderNo (order number)
- Long orderId
- Long customerId
- String cancelReason (CUSTOMER_REQUESTED, SYSTEM_INITIATED, FRAUD_FLAGGED, etc.)
- String cancelStatus (CANCELED, PENDING_REVIEW, REQUESTED)
- String cancelSource (CUSTOMER, SYSTEM, ADMIN)
- LocalDateTime cancelDate
- BigDecimal amount
- String remark

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/cancelations/summary` | Summary stats |
| POST | `/admin/cancelations/list` | List with filters |
| GET | `/admin/cancelations/{orderNo}` | Cancelation detail |

### Summary Response Fields

- totalCancelations: count of all cancellations
- pendingReview: count where status = PENDING_REVIEW or REQUESTED
- cancelationRate: (totalCancelations / totalOrders) * 100
- valueLost: sum of canceled order amounts

---

## Quick Setup

### Prerequisites

- Java 17+
- Spring Boot 3.5.0
- PostgreSQL 12+
- Maven 3.6+

### Installation

1. **Clone & Build**
   ```bash
   mvn clean install
   ```

2. **Configure Database**
   - Edit `application.properties` with your PostgreSQL credentials
   - Flyway will auto-create tables on startup

3. **Configure Email**
   ```properties
   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your_email@gmail.com
   spring.mail.password=your_app_password
   ```

4. **Start Application**
   ```bash
   mvn spring-boot:run
   ```

5. **Verify**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - API Docs: http://localhost:8080/v3/api-docs

### Testing

#### 1. Auth Flow

**Register**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Verify Email** (click link from email or use token)
```bash
curl -X GET "http://localhost:8080/api/auth/verify?token=550e8400-e29b-41d4-a716-446655440000"
```

**Login**
```bash
curl -X POST http://localhost:8080/api/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Response**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "a1b2c3d4-e5f6-7890...",
  "tokenType": "Bearer",
  "email": "test@example.com",
  "displayName": "testuser",
  "role": "USER"
}
```

#### 2. Returns Flow

**Create Return Request**
```bash
curl -X POST http://localhost:8080/admin/returns/user \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "customerId": 1,
    "productId": 1,
    "returnType": "RETURN",
    "reason": "Item defective",
    "amount": 99.99
  }'
```

**Get Summary**
```bash
curl -X GET http://localhost:8080/admin/returns/summary \
  -H "Authorization: Bearer {adminToken}"
```

**List Returns**
```bash
curl -X POST http://localhost:8080/admin/returns/list \
  -H "Authorization: Bearer {adminToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "page": 1,
    "size": 10,
    "status": "REQUESTED"
  }'
```

**Approve Return**
```bash
curl -X POST http://localhost:8080/admin/returns/RET-A1B2C3D4/approve \
  -H "Authorization: Bearer {adminToken}" \
  -H "Content-Type: application/json" \
  -d '{ "remark": "Approved for refund" }'
```

#### 3. Permissions Flow

**Login as Admin**
```bash
# Use built-in admin account (seed data)
curl -X POST http://localhost:8080/authorization \
  -H "Content-Type: application/json" \
  -d '{
    "CriteriaValue": "admin",
    "Password": "admin123"
  }'
```

**Create Function**
```bash
curl -X POST http://localhost:8080/api/v1/functions/create/ \
  -H "Authorization: Bearer {adminToken}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'funcCode=CUSTOM_DELETE&funcName=Custom Delete&description=Delete custom resource&module=CUSTOM'
```

**Assign to User**
```bash
curl -X POST http://localhost:8080/api/v1/user-permissions/create/ \
  -H "Authorization: Bearer {adminToken}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'userId=1&funcId=404'
```

**Wire Endpoint**
```sql
INSERT INTO api_permissions (method, api, func_id, is_active)
VALUES ('POST', '/api/v1/custom/resource/delete/', 404, true);
```

---

## Status Codes Reference

| Status | Scenario |
|--------|----------|
| 200 | Success (GET, POST with response body) |
| 201 | Created (POST creating resource) |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (missing/invalid auth) |
| 403 | Forbidden (permission denied) |
| 404 | Not Found (resource doesn't exist) |
| 409 | Conflict (duplicate email, already verified, etc.) |
| 410 | Gone (token expired) |
| 500 | Server Error |

---

## Database Schema Overview

### Auth Tables

- `users` - User accounts
- `verification_tokens` - Email verification tokens
- `refresh_tokens` - JWT refresh tokens
- `password_reset_tokens` - Password reset tokens

### Returns Tables

- `tbl_return_request` - Return requests
- `tbl_return_status_history` - Status change audit trail

### Permissions Tables

- `api_permissions` - Endpoint → Function mapping
- `function_permissions` - Function definitions
- `group_permissions` - Group → Function assignments
- `user_groups` - User → Group assignments
- `user_permissions` - User → Function direct grants

### Order Tables

- `tbl_order` - Orders
- `tbl_order_item` - Order line items
- `tbl_order_cancelation` - Cancellation records

---

## Important Notes

1. **Admin Bypass**: Any user with `ADMIN` authority skips all permission checks on `api_permissions` - this includes seeded admin account
2. **Email Security**: Forgot-password always returns 200 (prevents email enumeration)
3. **Token Rotation**: Refresh tokens are rotated on each use; old token is deleted
4. **Session Invalidation**: Password reset invalidates all refresh tokens, forcing re-login everywhere
5. **Soft Delete**: Products use soft delete (deleted flag, still in DB)
6. **Async Email**: Email sending is async (@Async) to not block requests
7. **Timestamp Auditing**: All entities have created_at, updated_at with automatic timestamps

---

## Troubleshooting

### Email not sending?
- Check application.properties for email credentials
- Enable "Less secure app access" on Gmail if using Gmail
- Use app-specific password (not main Gmail password)
- Check logs for MessagingException

### Token invalid after login?
- Verify JWT secret in application.properties matches
- Check token expiration (default 12000111 ms)
- Refresh token if access token expired

### Permission denied on endpoint?
- Check if user has ADMIN role (bypasses checks)
- Verify api_permissions row exists for endpoint
- Verify user has the required funcId (direct or via group)
- Check if grant is active (isActive=true)

### Database errors?
- Ensure PostgreSQL is running
- Check connection string in application.properties
- Run Flyway migrations: `mvn flyway:migrate`

---

## Next Steps

1. Configure your email provider
2. Update application.properties with your database
3. Run `mvn spring-boot:run`
4. Test auth endpoints
5. Test returns endpoints
6. Configure API permissions as needed

Refer to individual markdown files (Email.md, SERVICE_ORDER_TASKS.md, OPTIONAL_ATTRIBUTES_GUIDE.md) for detailed examples and request/response formats.


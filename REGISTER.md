# Register user

`POST /api/v1/public/register`

No `Authorization` header. The account is created disabled. The verification code is emailed and is not in this response. Open Mailpit at http://localhost:8025 when the local stack is running.

Gateway: `http://localhost:8080/api/v1/public/register`

Auth service: `http://localhost:8081/api/v1/public/register`

`POST /api/v1/public/accounts/register` runs the same create method and returns the same JSON. That route responds with HTTP 200. This route responds with HTTP 201.

## Request

`Content-Type: application/json`

```json
{
  "username": "mailpitcheck",
  "password": "Test-password-123!",
  "email": "mailpitcheck@example.test",
  "phone": "012345678",
  "full_name": "Mail Pit"
}
```

| Field | JSON name | Required |
| --- | --- | --- |
| Username | `username` | Used for the duplicate check |
| Password | `password` | Must not be blank |
| Email | `email` | Used for the duplicate check |
| Phone | `phone` | Database column is not null |
| Full name | `full_name` | Stored as sent |

## Response 201

The HTTP status is 201. The JSON field `code` is the string `"200"`. `data.password` is the stored bcrypt hash of the submitted password. `data.created` is a local date-time with no timezone. The user is not enabled yet.

```json
{
  "message": "User registered successfully. Please check your email to verify your account.",
  "code": "200",
  "data": {
    "id": 40,
    "username": "mailpitcheck",
    "password": "$2a$10$exampleHashNotARealPasswordValue",
    "email": "mailpitcheck@example.test",
    "phone": "012345678",
    "full_name": "Mail Pit",
    "roles": ["USER"],
    "created": "2026-10-03T14:02:26.149994472"
  }
}
```

## Response 400

The body is not JSON.

```json
{
  "timestamp": "2026-10-03T13:58:36.081851707",
  "status": 400,
  "error": "Malformed Request",
  "message": "Request body is malformed or missing",
  "path": "/api/v1/public/register",
  "errorCode": "MALFORMED_REQUEST",
  "debug_message": "JSON parse error: Unexpected character ('u' (code 117)): was expecting double-quote to start field name"
}
```

## Response 409

`phone` was omitted. The insert is rolled back.

```json
{
  "timestamp": "2026-10-03T13:58:45.22292061",
  "status": 409,
  "error": "Data Integrity Error",
  "message": "Database constraint violation",
  "path": "/api/v1/public/register",
  "errorCode": "DATA_INTEGRITY_ERROR",
  "debug_message": "could not execute statement [ERROR: null value in column \"phone\" of relation \"tbl_user\" violates not-null constraint"
}
```

## Response 500

These two cases throw before a row is saved. The HTTP status is 500. The reason is in `debug_message`.

Blank password:

```json
{
  "timestamp": "2026-10-03T14:02:25.722426899",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/v1/public/register",
  "errorCode": "INTERNAL_ERROR",
  "debug_message": "Password can't be blank or null"
}
```

Username or email already exists:

```json
{
  "timestamp": "2026-10-03T14:02:26.755577754",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/v1/public/register",
  "errorCode": "INTERNAL_ERROR",
  "debug_message": "Username or Email already exists."
}
```

SMTP login failure uses the same 500 body. `debug_message` is `Authentication failed`. That text is the mail server rejecting auth-service, not the new user's password. The insert is rolled back. Local Mailpit avoids that failure. After the auth-service image is rebuilt from current source, a mail failure returns HTTP 503 instead:

```json
{
  "timestamp": "2026-10-03T14:02:26.755577754",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Unable to send email. Please try again later or contact support.",
  "path": "/api/v1/public/register",
  "errorCode": "EMAIL_DELIVERY_FAILED"
}
```

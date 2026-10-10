# AI request processing

A caller asks `ai-service` to do one thing in plain language. `ai-inference` only names a tool and its parameters. `ai-service` decides whether that result may run, then calls one fixed client. The model never receives a URL, a bearer token, or permission to call another service.

```text
Angular
  -> API Gateway                 X-Request-ID, JWT
  -> ai-service POST /api/ai/execute
       1. Accept or replay the idempotency key
       2. List tools this actor may use
       3. POST /api/v1/ai/route on ai-inference
  -> ai-inference
       4. Build instructions separately from the user message
       5. Call OpenAI Responses
       6. Keep the result only when it matches a supplied tool
  -> ai-service
       7. Require confidence, authorize, and validate again
       8. Call one fixed Catalog or Order client
       9. Audit, and enqueue a notification for a completed write
```

`ai-inference` is decision support. `ai-service` is the authority.

## 1. Accept the caller

`POST /api/ai/execute` requires a valid enabled access JWT with a numeric `userId`. The body is only `message` (at most 4000 characters). Extra fields, including a caller-supplied `userId`, are rejected.

Headers:

- `Authorization` stays on this request. It is not forwarded to `ai-inference` or to OpenAI.
- `X-Request-ID` is reused when it is a single safe value. Otherwise the gateway or the service generates one.
- `traceparent` is forwarded when it is a valid W3C value. Neither service creates a new trace.
- `Idempotency-Key` is a UUID. When it is absent, `ai-service` generates an execution id.

The execution row is inserted before any tool runs. The same key from the same user returns the saved status and does not run the tool again. A replay does not return the previous business payload. The same key from another user is rejected.

## 2. Choose the tools the actor may see

`AiToolRegistry.available()` is the only catalog sent to the model. A tool is included when it is enabled and the actor's permission matches it. Admin tools are omitted for a non-admin. `ORDER_CANCEL`, `PAYMENT_REFUND`, and `ROLE_GRANT` are registered and disabled, so they are never sent and never dispatched.

Each forwarded tool has a name, a description, and parameter names mapped to `integer`, `number`, or `string`. The service name, HTTP method, and path stay in the registry.

## 3. Ask ai-inference

`AiInferenceClient` posts to `AI_INFERENCE_URL` + `/api/v1/ai/route`. Compose sets that URL to `http://ai-inference:8000`. The host default is `http://localhost:8000`. The Java client waits 25 seconds. The Python provider waits 20 seconds, so a provider timeout is reported by Python first.

The client reads the JSON fields `intent`, `confidence`, and `parameters`. It ignores `requiresConfirmation`. An intent name that is not a Java `AiIntent` becomes `UNKNOWN` with empty parameters, so a name such as `NOTIFICATION_LIST` cannot be executed. Provider failures become:

| Python result | Java code |
|---|---|
| HTTP 504 or `AI_PROVIDER_TIMEOUT` | `AI_PROVIDER_TIMEOUT` |
| HTTP 503 | `AI_NOT_CONFIGURED` |
| `AI_INVALID_RESPONSE` | `AI_INVALID_OUTPUT` |
| Any other non-200 | `AI_PROVIDER_FAILURE` |

The caller sees "AI intent detection is unavailable. Please try again later." Provider bodies are not returned.

## 4. Build the prompt

`ai-inference` checks the request before calling a model. The message must be 1–4000 characters. There may be at most 40 tools, each with a unique uppercase name. `UNKNOWN` is reserved and cannot be registered as a tool. Parameter types are `boolean`, `integer`, `number`, and `string`. A bad request is HTTP 422 `AI_INVALID_PARAMETERS`. The error body does not echo the message.

The system instructions are fixed rules plus the tool list as JSON. The user message is sent only as the OpenAI `input` field. It is not copied into the instructions. The rules tell the model to select one supplied tool, return JSON, and ignore requests to invent tools, endpoints, SQL, or authorization.

`/health` only shows that the process is up. `/ready` checks that `AI_PROVIDER` is `OPENAI` and that the API key and model are set. Neither check calls OpenAI.

## 5. Call the provider

The configured provider is OpenAI. The process keeps one `httpx` client. Each call posts to `https://api.openai.com/v1/responses` with `store` false, at most 1000 output tokens, a 3-second connect timeout, and the request timeout from `AI_REQUEST_TIMEOUT_SECONDS`. The response schema allows only the supplied tool names plus `UNKNOWN`, and only the supplied parameter names. Missing parameter values are null.

There is no retry. A timeout is HTTP 504 `AI_PROVIDER_TIMEOUT`. A transport or upstream failure is HTTP 502 `AI_PROVIDER_ERROR`. Output that is not completed JSON of the expected shape is HTTP 502 `AI_INVALID_RESPONSE`. A missing key is HTTP 503 and does not call OpenAI. The public message is always "AI routing is temporarily unavailable."

Logs record the request id, provider, selected tool, confidence, duration, and outcome. They do not record the API key, the Authorization header, or the user message.

## 6. Accept or discard the model result

`IntentService` does not trust the model. A usable selection must have matching `intent` and `tool`, and that name must be in this request's tool list. Confidence must be from 0 through 1 and at least `AI_MIN_CONFIDENCE` (0.70). Parameters must use the declared types. Null values are dropped. A non-null parameter that the selected tool does not declare is rejected. Strings must be non-empty, at most 4000 characters, and free of control characters. Integers must fit in a signed 64-bit value.

These results become HTTP 200 with `intent` `UNKNOWN`, `tool` null, confidence 0, and empty parameters:

- The tool was not supplied (`AI_TOOL_NOT_ALLOWED`), including a tool invented by a prompt injection.
- The parameters do not match the tool (`AI_INVALID_PARAMETERS`).
- Confidence is below 0.70 or outside 0 through 1 (`AI_UNKNOWN_INTENT`).
- `intent` and `tool` disagree.

When the model itself returns `UNKNOWN` with a confidence inside 0 through 1, that confidence is kept and its parameters are discarded. `ORDER_CANCEL`, `PROMOTION_CREATE`, `PROMOTION_DISABLE`, and `PAYMENT_REFUND` set `requiresConfirmation` to true. The flag is computed here. A value sent by the model is rejected as an unexpected field.

## 7. Authorize again in Spring

`ai-service` treats the Python response as untrusted input. `requireConfident` rejects a missing result, a non-finite confidence, or a confidence outside 0.80 through 1 as `UNCERTAIN_INTENT`.

`AiToolRegistry.authorize` then checks the intent again:

- An unknown intent is `UNKNOWN_INTENT`.
- A disabled tool is `TOOL_DISABLED`.
- A tool the actor cannot use is `PERMISSION_DENIED`.
- A high-risk write is `CONFIRMATION_REQUIRED` and is not dispatched. There is no confirmation workflow.

`validate` rejects unknown keys, missing required values, non-integers, ids below 1, a low-stock threshold outside 0 through 1,000,000, a discount that is not greater than 0 and at most 100 with at most 4 decimal places, blank or control-character text, an order number outside `[A-Za-z0-9_-]{1,80}`, and a promotion window where `startAt` is not before `endAt`.

Python's 0.70 floor only removes weaker matches. Execution still requires 0.80.

## 8. Execute one fixed call

`AiToolExecutor` switches on the enum. It does not read a URL from the model. The call uses the caller's bearer token so the downstream service applies its own authentication.

| Tool | Client call |
|---|---|
| `PRODUCT_GET` | catalog product by `id` |
| `PRODUCT_SEARCH` | one product search page, size 20 |
| `SKU_GET` | catalog SKU by `skuId` |
| `INVENTORY_GET` | inventory by `skuId` |
| `INVENTORY_LOW_STOCK` | first 20 rows, default threshold 10 |
| `ORDER_GET` | the caller's order by `orderNumber` |
| `PROMOTION_GET` | admin promotion by `id` |
| `PROMOTION_CREATE` | one draft promotion and its SKU assignment |
| anything else | `TOOL_DISABLED` |

A null body, an application code other than 200 or 201, an HTTP status of 400 or more, or a non-zero `errorCode` is `DOWNSTREAM_REJECTED`. The business payload is not sent back to the model.

## 9. Record the outcome

The execution status, audit row, and notification outbox row commit together. History stores the intent, status, and error code. It does not store the prompt, the parameters, or the downstream body.

Reads do not emit a notification. A successful or failed write enqueues one outbox row. The publisher sends one row at a time to `notification.events.v1` after the database commit. `notification-service` creates one in-app notification per event id.

A read that fails is `FAILURE`. A write that was dispatched and then times out or returns a 5xx leaves the outcome `UNKNOWN`: the downstream service may have committed. Check execution history and the catalog audit by request id before submitting a new idempotency key.

HTTP status from `POST /api/ai/execute`:

| Status | HTTP | Meaning |
|---|---|---|
| `SUCCESS` | 200 | The fixed client returned a usable body |
| `RUNNING` | 202 | This key was already accepted and is still running |
| `NEEDS_INPUT` | 422 | The intent or parameters need to be clarified |
| `DENIED` | 403 | The tool is disabled or the actor may not use it |
| `UNKNOWN` | 409 | A write was dispatched and its outcome is uncertain |
| `FAILURE` | 502 | The provider or a downstream call failed safely |

`GET /api/ai/executions/{id}` returns metadata for the caller's execution only.

# ai-inference

## Gemini configuration

Set `AI_PROVIDER=GEMINI`, `GEMINI_MODEL=gemini-3.1-flash-lite`, and `GEMINI_API_KEY` in the ignored root `.env` for Compose, or the ignored `ai-inference/.env` for local execution. Keep the key out of committed files. The provider uses Google's fixed HTTPS endpoint with `x-goog-api-key` header authentication, bounded streamed responses, explicit deadlines and sanitized errors. It supports structured tool routing, Excel mapping/explanation assistance, product-image suggestions and conversational chat. Backend authorization, confirmation and import approval remain authoritative. The configured Flash-Lite model passed live routing and chat checks; the model can be changed through `GEMINI_MODEL`.

Rebuild/recreate only `ai-inference` using `docker compose -f compose.yaml -f compose.ai.yaml -f compose.local.yaml up -d --build --no-deps ai-inference`. Do not add `compose.ollama.yaml` for Gemini: that optional file explicitly selects Ollama. `/api/v1/ai/info` reports the active model without credentials; `/ready` checks configuration, not Google's quota or key validity.

Implementation reference: [Gemini structured outputs](https://ai.google.dev/gemini-api/docs/generate-content/structured-output). Model availability is checked with Google's model list; generation access must also be tested because listed legacy models may be unavailable to new users.

Decision support for the E-Shop AI orchestrator. This service chooses one supplied tool and its parameters. Spring Boot `ai-service` remains the authority for authentication, authorization, validation, execution, audit, and notifications.

```text
API Gateway
    ↓
ai-service (Spring Boot)
    ↓
POST /api/v1/ai/route
    ↓
ai-inference (this service)
    ↓
structured intent
    ↓
ai-service tool registry, permission check, and fixed client call
```

The model cannot add a tool, choose a URL, or run SQL. A tool that was not in the request becomes `UNKNOWN`. Write tools `ORDER_CANCEL`, `PROMOTION_CREATE`, `PROMOTION_DISABLE`, and `PAYMENT_REFUND` set `requiresConfirmation` to true. Spring Boot makes the final risk decision.

## API

`POST /api/v1/ai/route`

```json
{
  "message": "Show my unread notifications",
  "tools": [
    {
      "name": "NOTIFICATION_LIST",
      "description": "Get notifications for current user",
      "parameters": { "unreadOnly": "boolean" }
    }
  ]
}
```

```json
{
  "intent": "NOTIFICATION_LIST",
  "tool": "NOTIFICATION_LIST",
  "confidence": 0.98,
  "parameters": { "unreadOnly": true },
  "requiresConfirmation": false
}
```

An unmatched, low-confidence, or disallowed model result is HTTP 200 with `intent` `UNKNOWN`, `tool` null, and empty parameters. That includes prompt injection that names a tool the request did not supply.

Provider failures use a fixed message and do not include upstream bodies:

| HTTP | code | When |
|---|---|---|
| 504 | `AI_PROVIDER_TIMEOUT` | Provider or routing deadline exceeded |
| 502 | `AI_PROVIDER_ERROR` | Provider transport or upstream failure |
| 502 | `AI_INVALID_RESPONSE` | Provider output is not usable JSON |
| 503 | `AI_PROVIDER_ERROR` | Provider is not configured |
| 422 | `AI_INVALID_PARAMETERS` | The routing request itself is invalid |

`AI_TOOL_NOT_ALLOWED`, `AI_INVALID_PARAMETERS` (for model arguments), and `AI_UNKNOWN_INTENT` are decision reasons. They are logged and returned as `UNKNOWN` so they cannot be executed.

Send `X-Request-ID`. A missing or unsafe value is replaced and returned on the response. A valid W3C `traceparent` is echoed and forwarded to the provider. This service does not create traces.

`GET /health` reports that the process is up. `GET /ready` checks provider configuration and does not call the model. OpenAI and Gemini require their configured API key and model; Ollama requires its model. Unsupported provider names are not ready.

## Configuration

See `.env.example`. Do not commit real keys.

```text
AI_PROVIDER=OPENAI
AI_REQUEST_TIMEOUT_SECONDS=20
AI_MIN_CONFIDENCE=0.70
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4.1-mini
LOG_LEVEL=INFO
```

The OpenAI call uses the Responses API, `store=false`, a strict schema limited to the supplied tools, and one shared `httpx` client. The user message is the `input`. It is not copied into the system instructions.

Logs include the request id, provider, selected tool, confidence, duration, and outcome. They do not include the API key, Authorization header, or the user message.

## Run

```powershell
python -m venv .venv
.venv\Scripts\pip install -r requirements-dev.txt
.venv\Scripts\pytest
.venv\Scripts\uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Compose publishes the service on `127.0.0.1:8000` and points `ai-service` at `http://ai-inference:8000`. Do not expose this port on a public network. It does not check caller permissions.

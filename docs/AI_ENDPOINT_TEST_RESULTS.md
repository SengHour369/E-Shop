# Real endpoint smoke test — 2026-10-09

These checks used real HTTP requests against the running local services.
They did not use mocked controllers or model responses.

## Chat request

```http
POST http://localhost:8080/api/ai/chat
Content-Type: application/json
X-Request-ID: real-chat-smoke-20261009

{"message":"Search products named shoes","language":"en"}
```

Actual response: HTTP 401, Content-Type application/json.

```json
{
  "status": 401,
  "requestId": "real-chat-smoke-20261009",
  "error": "UNAUTHORIZED",
  "message": "Missing or invalid Authorization header"
}
```

The request ID propagated correctly. The new public guest-chat behavior did
not pass on this running deployment. The existing api-gateway and AI-service
containers have been running for two days and have not been rebuilt with the
new implementation.

## Other checks

| Target | Actual result |
| --- | --- |
| http://localhost:8080/api/ai/chat | E-Shop gateway, HTTP 401 |
| http://[::1]:8080/api/ai/chat | E-Shop gateway, HTTP 401 |
| http://127.0.0.1:8080/api/ai/chat | Unrelated Apache server, HTML HTTP 404 |
| http://127.0.0.1:8000/health | Connection refused; inference service not running |
| http://127.0.0.1:11434/api/tags | Connection refused; Ollama not running |
| http://localhost:8085/api/ai/chat | Unrelated Jenkins server, HTTP 403 |

Port conflicts mean localhost/IPv6 and 127.0.0.1 reach different services on
8080. Port 8085 is occupied by Jenkins; do not start a host AI service there
without selecting another port. The Docker AI service can stay internal.

## Model configuration status

No OPENAI_API_KEY was configured in the current process or the existing AI
container. No root .env or ai-inference/.env file was present. No local Ollama
server was reachable. Secret values were not printed.

## Required before a successful live assistant test

1. Configure a real provider: OPENAI with OPENAI_API_KEY and OPENAI_MODEL, or
   OLLAMA with an installed model and a running server. Keep secrets in local
   configuration; do not paste them into chat.
2. Build and start the updated AI inference, AI service, gateway, and affected
   domain services using the existing local release/development procedure.
   Preserve the existing database volumes and credentials.
3. Confirm inference /health and /ready, then repeat the guest chat request
   through the gateway and verify the result is backed by real catalog data.
4. Test signed-in customer/admin paths using actual login credentials and
   assigned function grants. No real login credentials were provided for this run.

No existing containers were rebuilt, no model calls were made, and no domain
business data was mutated in this smoke test. The current result is NOT an
end-to-end pass, despite the separately passing automated tests.

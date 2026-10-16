# Gemini chat repair and live verification

Verified locally on 2026-10-17 (Asia/Phnom_Penh).

## Cause and fix

The running AI inference container selected `OPENAI` with no provider key. The owner's legacy `env` file contained Gemini configuration, but Docker Compose does not automatically read that filename. Gateway chat returned `AI_NOT_CONFIGURED`.

`run-docker.sh` now explicitly loads `.env`, or legacy `env` when `.env` is absent, then overlays ignored `.env.local`. `ESHOP_ENV_FILE` selects one explicit file. The missing optional `compose.docker-local.yaml` no longer prevents startup. Configuration values are not printed or shell-sourced. Compose checks `/ready` for inference configuration before starting dependent AI services.

The supplied Gemini key was accepted by Google's models endpoint. The configured `gemini-3.1-flash-lite` generation endpoint repeatedly returned HTTP 503 / UNAVAILABLE (high demand). `gemini-flash-lite-latest` returned a genuine generated answer. The ignored `.env.local` now overrides only provider/model selection; credentials remain in the existing local configuration. Only `ai-inference` was rebuilt/recreated, with database and other service containers retained.

## Actual responses

- Inference `/ready`: HTTP 200, READY.
- Inference `/api/v1/ai/info`: GEMINI, `gemini-flash-lite-latest`, configured=true.
- Gateway `POST /api/ai/chat`, English greeting: HTTP 200, ConversationCard, SUCCESS, real Gemini answer; request ID `ac21d2a1-a778-43a8-a151-b7630b304f7e`, 16.43 seconds.
- Gateway `POST /api/ai/chat`, Khmer shopping greeting: HTTP 200, ConversationCard, SUCCESS, Khmer Gemini answer; request ID `68dbf438-0cf2-40b9-8bfc-844b34afcfd6`, 15.24 seconds.

These were guest conversation requests through the actual running gateway and AI service. They did not create orders, change customer accounts or execute write tools. No guarantee is made that the upstream provider will remain available during future requests.

## Regression checks

`python -m pytest -q ai-inference/tests tests/test_run_docker.py`: **107 passed**. Launcher tests use fake Docker/curl and verify legacy file loading, override ordering, optional Compose handling and fail-fast behavior for a missing explicitly selected environment file. `bash -n run-docker.sh` and scoped diff checks passed.

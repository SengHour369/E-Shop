# Gemini Live calling — implementation and verification

For the current product discovery, selection and admin extension, see [product-discovery.md](product-discovery.md). The earlier verification sections below describe their original deployment stage.

Implemented in the existing repositories on 2026-10-23. **Disabled by default; real speech acceptance is not complete.** No usable Gemini key was present in the process environment or the existing local backend environment files. No Google voice session, real Khmer pronunciation assessment, or live voice-triggered Bakong payment was run.

## Provider verification

Google's [pricing page](https://ai.google.dev/gemini-api/docs/pricing) lists `gemini-3.1-flash-live-preview` with free audio input/output. The [Live overview](https://ai.google.dev/gemini-api/docs/live-api) describes bidirectional audio, transcription and interruption. The [capabilities guide](https://ai.google.dev/gemini-api/docs/live-api/capabilities) lists Khmer (`km`) and English, and supports language selection through instructions. This is documentation verification, not verification of this project's access or quota.

The backend checks model metadata for `bidiGenerateContent` before reserving a call. Token issuance/Live setup can still fail because of project access or quota. There is no paid fallback and no retry loop. Only the documented, reviewed model is allowlisted; changing `GEMINI_LIVE_MODEL` to an unreviewed model disables calls. To admit a future model, review its free audio pricing and capabilities first and update the server allowlist and tests.

The official `@google/genai` SDK installed here uses **v1alpha** for constrained ephemeral Live connections. Backend provisioning deliberately uses the matching REST wire format, including `bidiGenerateContentSetup`. See [ephemeral tokens](https://ai.google.dev/gemini-api/docs/live-api/ephemeral-tokens). No permanent API key is sent to Angular.

## Configuration

Use `ai-service/.env.example` as a reference; Spring does not automatically load that file. Export the variables for local startup or supply them through the existing Compose environment. `compose.ai.yaml` passes the Gemini settings to ai-service.

| Variable | Default | Purpose |
| --- | --- | --- |
| `GEMINI_API_KEY` | empty | Server-only Developer API key |
| `GEMINI_LIVE_ENABLED` | `false` | Explicit feature enablement |
| `GEMINI_FREE_PROJECT_VERIFIED` | `false` | Operator attestation that this specific project is unbilled and has eligible free Live quota |
| `GEMINI_LIVE_MODEL` | `gemini-3.1-flash-live-preview` | Approved model |
| `AI_VOICE_MAX_CALLS_PER_DAY` | `5` | Validated range 1–5 |
| `AI_VOICE_MAX_DURATION_SECONDS` | `300` | Validated range 30–300 |
| `REDIS_HOST`, `REDIS_PORT` | existing settings | Shared quota/session storage |

Property binding is under `gemini.live`. `free-only=true`, `paid-fallback-enabled=false`, and `max-concurrent-calls=1` are validated invariants. They cannot be relaxed to enable charging or concurrent calls. No code enables billing. The API does not provide a reliable billing-tier/remaining-Live-quota preflight through model metadata; do not set the verification flag without checking this project's Google AI Studio quota and billing state. A key attached to a billed project can incur provider charges despite application-side limits; application code cannot establish an unbilled project merely from its key.

All new endpoints require the existing JWT through gateway and ai-service security. Guests retain their existing text/voice-message features but cannot start Gemini calls. Deploy the app over HTTPS/WSS; localhost is suitable for development. Permit `wss://generativelanguage.googleapis.com` in any deployment CSP `connect-src` and serve `/audio/eshop-capture.js` from the frontend origin.

## Existing chat and commerce integration

The header's Call AI control opens an overlay with a language selector and privacy notice. Start/Retry explicitly initiates microphone permission and calling. The original chat, product cards, history, checkout and QR components are retained. The overlay can be hidden to review cards while the call continues; closing the chat, changing accounts/history, or destroying the component ends media resources. Voice-message capture is blocked while a live call is active.

AudioWorklet captures microphone samples; the fallback uses ScriptProcessor only when AudioWorklet is unavailable. Streaming resampling handles 44.1/48 kHz devices and emits signed little-endian PCM16 at 16 kHz. Provider PCM is decoded at its declared rate (24 kHz default), queued using AudioContext time, and flushed on interruptions. The meter uses actual microphone RMS; no synthetic waveform replaces capture. Tokens and audio are never logged. Raw audio is not persisted. The product discovery extension places transcripts in existing chat history; see the current extension notes. The overlay keeps at most 40 transcript entries in component memory.

Registered customer tool aliases cover products/SKUs/stock, cart, checkout preview/order staging, owned orders/cancellation, KHQR/status and notifications. Dispatch reuses `AiController.tool`, registry validation, live permissions, downstream ownership checks, execution history, confirmation staging, request caching and audit. Tool-call IDs derive stable session-scoped idempotency keys. `get_saved_addresses` uses the existing auth-service address API with `CurrentActor.userId()`; only ID/default references reach Gemini. Private address details and QR data remain in existing UI flows.

`prepare_checkout` stages the existing `ORDER_CREATE` confirmation using a valid owned checkout review. There is deliberately **no model-callable `confirm_checkout`**: speech saying “yes” cannot authorize its own mutation. The customer reviews and clicks the existing confirmation card. The assistant then requests fresh state through tools. This also preserves confirmation for cart changes, order cancellation and KHQR generation. Payment UI polls/verifies using the existing Payment Service; Gemini can fetch authoritative payment status. A spoken claim, a generated QR, or a client message never changes payment state. Live payment-provider verification still requires the shop's existing Bakong configuration.

Only reference IDs from the current actor's existing chat context enter the Live system instructions. Voice tool results update that same Redis context, providing product/SKU/order continuity back to text chat. Prior free-form text dialogue is not uploaded to Google. Tool results sent to the model use an explicit field allowlist; contacts, full addresses, payment payloads and credentials are excluded. The UI receives the actual authorized result for its existing renderer.

## Quota semantics and direct-connection limits

Redis Lua scripts atomically reserve the user's active slot and daily budget across devices/instances. Business days reset in Asia/Phnom_Penh. A successful browser Live setup acknowledgement commits one used call exactly once. Failed model/key checks do not reserve anything; failed token provisioning releases its reservation. Redis errors fail closed.

An issued credential reserves a daily slot until committed or the day resets. Unacknowledged issued credentials remain conservatively reserved, since a modified browser could omit the acknowledgement while using a copied credential. `usedToday` counts acknowledgements; `reservedToday` is shown separately in the API, and `remainingToday` subtracts both. This can reduce availability after a connection failure **without falsely counting the failed call as successful**. No quota data supplied by Angular is trusted for enforcement. Successful provider connection itself is not independently attested by Google to this backend.

Credentials are single-use, restricted to the approved model/audio/system/tools, admit new connections for only 30 seconds, and expire at the backend session's 300-second maximum deadline. No session resumption is enabled. The browser stops audio and closes Live at the deadline; backend commerce tools reject expired or ended sessions. Redis TTLs clean sessions automatically. Ending a call retains its concurrency lease until the original deadline because the backend cannot revoke a copied direct token.

**A credential expiration is not a proven hard provider-side WebSocket termination.** This implementation cannot remotely kill a modified browser's already-open Google connection or cryptographically verify that its tool responses are unchanged. A strict adversarial five-minute transport cutoff needs a server-mediated audio connection and conflicts with the requested direct browser-to-Gemini architecture. Do not claim that this integration guarantees such a cutoff. Application quotas are independent of Google's quota.

## Verification

The repository actually targets Java 17 (Spring Boot 3.5.0); its Java version was preserved. Use the installed Java 17 runtime for Maven: the host's Java 25 compiler rejected this repository's release-17 target.

Commands run:

```bash
VOICE_REDIS_TEST=true JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH mvn -pl ai-service -am verify -q
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH mvn package -DskipTests -q
```

Frontend commands, from `/home/senghour/Desktop/Order_Shop_Frontend`:

```bash
npm run build
npm test -- --include='src/app/features/assistant/**/*.spec.ts'
```

- Shared/backend verification: 225 tests discovered, 201 passed, 24 skipped, no failures/errors. Existing environment-dependent skips remain; they do not establish external integration success.
- Real Redis tests ran against local Redis using unique isolated test-user key namespaces. They cover atomic contention across 16 attempts, acknowledgement idempotency, daily limits, provisioning-failure refunds, retained leases, and owner isolation.
- Assistant frontend suite: 97 tests passed across 18 files. Includes audio encoding/resampling, mocked Live connection, actual sample-to-SDK dispatch, backend tool-response relay, Khmer fragments, interruption, mute, disabled preflight, permission denial and cleanup. The provider is mocked in the bridge test; this is not external voice verification.
- Backend reactor package and frontend production build pass. Angular reports a 650 kB initial-size warning and an SDK `p-retry` CommonJS warning; the 1 MB error budget passes.
- The existing payment-authority test needed `provideRouter([])` to render its RouterLink. Its assertions now pass, including success only after verified payment.

Remaining real-device acceptance: after verifying an unbilled project and configuring a key, use the Khmer phrases in `khmer-regression.json` with actual microphone speech. Check exact transcription, audible pronunciation, 42/៤២, USD/KHR amounts, mixed language, English, barge-in, headphone/echo behavior, permissions, device disconnects, and iOS/Android playback. Use real catalog stock and owned address/order data; confirm changes in the existing UI. Confirm pending stays pending and only trusted Bakong verification permits success. No pronunciation findings can be reported before those real tests.

## Changed-file manifest

Existing uncommitted changes were preserved. Backend remained on its existing dirty `main` checkout rather than switching to `Dev_Senghour`; frontend was already on `dev_senghour`. The pre-existing changes to `AiVoiceControllerTest.java` were not authored by this task.

Backend paths, relative to `/home/senghour/Desktop/E-Shop`:

- `.gitignore`
- `ai-service/.env.example`
- `ai-service/src/main/resources/application.yml`
- `ai-service/src/main/java/com/example/eshop/ai/client/AuthToolClient.java`
- `ai-service/src/main/java/com/example/eshop/ai/service/AiVoiceAudit.java`
- `ai-service/src/main/java/com/example/eshop/ai/voice/VoiceProperties.java`
- `ai-service/src/main/java/com/example/eshop/ai/voice/GeminiVoiceTokenService.java`
- `ai-service/src/main/java/com/example/eshop/ai/voice/VoiceSessions.java`
- `ai-service/src/main/java/com/example/eshop/ai/voice/VoiceTools.java`
- `ai-service/src/main/java/com/example/eshop/ai/voice/LiveVoiceController.java`
- `ai-service/src/test/java/com/example/eshop/ai/voice/VoiceSecurityTest.java`
- `ai-service/src/test/java/com/example/eshop/ai/voice/VoiceRedisIntegrationTest.java`
- `ai-service/src/test/java/com/example/eshop/ai/voice/GeminiVoiceTokenTest.java`
- `ai-service/src/test/java/com/example/eshop/ai/voice/LiveVoiceControllerTest.java`
- `compose.ai.yaml`
- `docs/voice/gemini-live.md`
- `docs/voice/khmer-regression.json`

Frontend paths, relative to `/home/senghour/Desktop/Order_Shop_Frontend`:

- `package.json`
- `package-lock.json`
- `public/audio/eshop-capture.js`
- `src/app/features/assistant/assistant.api.ts`
- `src/app/features/assistant/customer-chat/customer-chat.ts`
- `src/app/features/assistant/customer-chat/customer-chat.html`
- `src/app/features/assistant/customer-chat/live-audio.ts`
- `src/app/features/assistant/customer-chat/live-audio.spec.ts`
- `src/app/features/assistant/customer-chat/live-voice.ts`
- `src/app/features/assistant/customer-chat/live-voice.spec.ts`
- `src/app/features/assistant/customer-chat/live-bridge.spec.ts`
- `src/app/features/assistant/customer-chat/voice-call.ts`
- `src/app/features/assistant/payment-authority.spec.ts`

## Local runtime correction — 2026-10-23

The frontend at `http://192.168.18.176:40673` was calling an older AI container whose JAR did not contain `LiveVoiceController`. The shared generic exception handler returned 500 for those missing routes. Rebuilt and recreated only `ai-service` using the existing Compose overlays and environment files. Authenticated GET requests through both port 8080 and the exact frontend origin now return 200 for `/api/ai/voice/config` and `/api/ai/voice/usage`. Configuration correctly reports `enabled:false`; no Google calls or quota-consuming sessions were started. The HTTP LAN origin still needs HTTPS for browser microphone access.

## Activation — 2026-10-23

After the owner confirmed that Google project billing is disabled, enabled `GEMINI_LIVE_ENABLED=true` and `GEMINI_FREE_PROJECT_VERIFIED=true` in the ignored local environment override and recreated only ai-service. The existing runtime key was preserved. Runtime configuration now reports `enabled:true`. A real provider test successfully provisioned a model-restricted, single-use ephemeral token and received Gemini Live `setupComplete` using the official SDK. The test sent no microphone audio, invoked no commerce tools, and closed its connection immediately. This establishes token provisioning and Live connection access, not audible Khmer quality, commerce acceptance, or a guarantee of future remaining quota. A synthetic identity was rejected by the existing shop permission lookup; no authorization check was relaxed.

For this computer, open `http://localhost:40673` to use the browser's localhost microphone exception. `http://192.168.18.176:40673` is an insecure LAN origin and requires HTTPS for microphone capture on other devices.

## Retry conflict correction — 2026-10-23

A user session was provisioned and then ended before acknowledgement. Its direct credential lease correctly remained active, so repeated attempts returned 409. The voice controller now returns a safe specific message, `retryAfterSeconds`, and a `Retry-After` header instead of the shared generic “Conflict” response. Usage exposes the active lease TTL; the frontend shows a real wall-clock countdown and blocks repeated Start attempts until it expires. No credential lease is cleared early and no concurrency or daily-quota protection is bypassed. Provider smoke testing with all 16 actual customer function declarations received `setupComplete`; the original browser disconnect could not be reproduced by that server-side test.

Retry deployment verification: 19 voice backend tests and 44 voice frontend tests passed; Angular production build passed. Over the actual HTTPS origin, an isolated synthetic lease returned usage 200 with `retryAfterSeconds:30`, and session creation returned 409 with the explicit active-call reason and matching `Retry-After:29`. After removing only that test lease, usage reported zero retry delay. The original customer's expired lease was not manually deleted or its daily quota reset.

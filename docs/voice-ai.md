# Voice in the existing customer assistant

Voice input is a draft-producing media adapter. It never calls commerce tools, creates conversations, confirms actions, or submits chat requests. After recording, the existing textarea contains the transcript for review and editing. Send uses the existing AssistantApi.chat flow, identity, conversationId, quota, history, orchestrator, tool permissions, confirmation IDs, and idempotency keys. Existing supported shopping commands therefore have the same behavior for typed and transcribed input.

## Deployment

Set these **server-side** variables on ai-service (passed through compose.ai.yaml):

- `AI_VOICE_TRANSCRIPTION_URL`: full URL of a trusted multipart speech transcription endpoint.
- `AI_VOICE_API_KEY`: optional bearer credential, never supplied to Angular.
- `AI_VOICE_MODEL`: optional provider model identifier.
- `AI_VOICE_FFPROBE`: ffprobe executable path, default `ffprobe`.

The provider contract is a POST with multipart `file`, `language` (`en` or `km`), and optional `model`. It must return JSON `{ "text": "..." }`. Configure a provider and model that support both requested languages. Use HTTPS for external provider connections and deploy the frontend behind HTTPS. Localhost browser development is permitted by browsers' secure-context rules. No provider is silently selected; an unconfigured server returns 503 and users can continue typing.

The ai-service Docker image includes ffmpeg/ffprobe. Install ffprobe separately for local Java execution. Audio is limited to 5 MiB and 60 seconds (one second of container timing tolerance), with a 60-second browser stop. Multipart upload limits also apply. Supported recordings: WebM/Opus, MP4, Ogg, WAV. ffprobe validates audio/container metadata and packet timestamps rather than trusting client duration. Inspection is limited to five seconds; provider connection/read timeouts are five/30 seconds. The gateway uses the existing 60-second chat route timeout for transcription.

`POST /api/ai/voice/transcriptions` allows the same optional authentication as customer chat. Supplied JWT/cookie credentials still pass through existing authentication. It uses the existing Redis actor/IP rate limiter, failing closed if unavailable. The accepted media request is audited using the existing audit service without audio or transcript. Media conversion does not charge the conversation quota; Send does. Transcription is a read-only operation, so a transport retry can repeat conversion but cannot execute a shopping mutation. Existing confirmation and commerce idempotency handling are unchanged.

Recordings and probe metadata are held in temporary files during validation and deleted in finally blocks; no recording is retained as chat history. Use an ephemeral tmpfs for `java.io.tmpdir` in production, with restrictive service account access. A process crash can bypass normal cleanup; ephemeral storage prevents retention across restarts. Do not enable request-body, multipart, or provider-body logging. Configure provider retention and privacy controls for your deployment; the provider necessarily receives the recording. The reviewed transcript becomes normal chat text and follows existing chat history policy. Audio/transcripts are excluded from voice audit metadata and error messages. Provider errors are replaced with safe user-facing messages.

## Playback and recording lifecycle

Playback uses browser/system speech synthesis. Play chooses an installed English or Khmer voice based on the response text and shows a clear error if no matching voice is available. Voice availability depends on the device; installing/configuring a Khmer voice may be necessary. A device voice may use its vendor's network speech service. No guaranteed Khmer voice or server synthesis fallback is bundled. Play, Pause, Resume, Stop are explicit controls; automatic playback starts disabled and can be enabled for the current chat instance.

Microphone permission is requested only after clicking Record. A real analyser meter, timer, Stop, Discard, and pending-upload Cancel are added around the existing composer. Close, Escape, component destruction, account changes, new chat, and history switches release media/cancel pending results and speech. A late permission grant after closing is immediately stopped. Text drafts remain available during errors.

## Verification

Automated tests cover malformed and oversized/empty input, unsupported language/type, server rate limiting, duration rejection, provider multipart/bearer forwarding, no-store transcript responses, permission denial, microphone cleanup, late permission resolution, discard without upload, editable Khmer transcript delivery, and speech playback controls. Existing customer chat and backend confirmation/isolation regression tests are also run.

Before rollout, validate real speech accuracy with the configured provider using Khmer and English recordings on desktop Chrome/Firefox and mobile Safari/Chrome. Verify permission denial, system voices, background/lock behavior, silence, network interruption, and sensitive command confirmations. Automated tests use simulated media and a local provider stub; they do not certify real microphones or provider language accuracy.

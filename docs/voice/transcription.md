# Voice-message transcription

The composer microphone records a short WebM/MP4/OGG clip and sends it to `/api/ai/voice/transcriptions`. This is separate from the Call AI Live WebSocket session.

The reported 503 was caused by an empty `AI_VOICE_TRANSCRIPTION_URL`. The existing upload handler rejected the request before reading its audio. A Google-backed path now handles authenticated uploads when no custom transcription URL is configured.

Configuration:

```dotenv
GEMINI_TRANSCRIPTION_ENABLED=true
GEMINI_TRANSCRIPTION_MODEL=gemini-3.1-flash-lite
GEMINI_API_KEY=<server-only key>
GEMINI_FREE_PROJECT_VERIFIED=true
```

`gemini-3.1-flash-lite` is an explicitly reviewed free-tier input/output model on Google's [pricing page](https://ai.google.dev/gemini-api/docs/pricing). Google's [audio documentation](https://ai.google.dev/gemini-api/docs/audio) supports audio-to-text and WebM audio. A real generated English WebM sample was accepted by this model through `generateContent` and transcribed as “Hello, I would like to search for black shoes.” This does not guarantee remaining quota. No paid fallback or alternate-model retry is implemented.

The same unbilled-project attestation used for Live calling is required. Both new variables are passed to ai-service in `compose.ai.yaml`; local overrides enable them. A custom `AI_VOICE_TRANSCRIPTION_URL`, if explicitly supplied, retains its original routing behavior. Google-backed transcription requires a valid signed-in user; anonymous users cannot spend the project's Google quota.

Uploads retain the existing 5 MB / 60-second limits, decoded ffprobe validation, per-actor rate limit, temporary-file cleanup and audit. Only validated audio is sent to Google. The permanent key stays on the server. No commerce tools are declared, and the transcription prompt requests only spoken words, preserving Khmer/English and mixed product names. Reasoning parts are excluded from the returned text. The text remains an editable draft and is never submitted as a chat message automatically.

The frontend displays safe provider errors, sign-in requirements, unclear speech, and free quota exhaustion. A provider 429 is never retried indefinitely. Audio is processed using Google's free tier, so the composer includes a privacy notice; avoid speaking private account or payment details. The old custom provider route remains compatible.

Automated checks include: existing upload validation and custom provider behavior; authenticated Gemini routing and anonymous denial; approved-model/unbilled-project gating; inline audio request format; Khmer response extraction; thought exclusion; silence; and redacted quota failures. Live speech quality still needs human review, especially Khmer pronunciation/transcription and mixed-language numbers.

The initially tested 3.8 model returned high-demand errors. Before delivery, the configured model was deliberately changed to the existing shop's free `gemini-3.1-flash-lite`; this is a fixed choice, not an automatic runtime fallback. Only that reviewed transcription model is allowlisted. Live calling keeps its separate model unchanged.

## Deployed verification

A real generated English speech sample was encoded to WebM/Opus and uploaded with multipart field `audio`, filename `recording`, MIME `audio/webm;codecs=opus`, and language `en` through `https://192.168.18.176:8443/api/ai/voice/transcriptions`. The deployed endpoint returned HTTP 200 and `Hello, I would like to search for black shoes.` in about 8 seconds. This was a generated, non-sensitive test fixture, not the user's pasted binary or a real Khmer recording. Nine targeted backend tests and 64 frontend voice/admin tests passed; both the Docker AI build and Angular production build passed.

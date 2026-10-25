# Voice transcription timeout handling

The transcription route `/api/ai/voice/transcriptions` shares the long AI conversation
route with explicit order -25, ahead of the generic AI route (order 0). It uses the
`aiChatServiceCB` circuit breaker with a 55-second time limit and a 60-second HTTP
response timeout. This prevents the generic AI circuit breaker's 30-second fallback
from interrupting the request.

The Gemini client has a 5-second connect timeout and 40-second read timeout. The browser
waits up to 50 seconds. Socket/provider timeouts return HTTP 504 with a short-recording
retry message. Logs include exception class and root-cause class only, never API keys,
audio contents or private provider response bodies. Quota failures remain HTTP 429;
other upstream outages remain HTTP 503. No automatic provider retries or paid fallback
are introduced.

Deploy both `ai-service` and `api-gateway` for the route change, and rebuild the frontend
for its 50-second wait and timeout message. Google availability is still external; the
change fixes premature timeout ordering and improves timeout reporting.

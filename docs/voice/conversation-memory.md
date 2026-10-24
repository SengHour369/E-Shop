# Persistent conversation memory

Chat messages and Gemini Live text transcripts are stored in `ai_memory_conversation` and
`ai_memory_turn` in the AI service PostgreSQL database. They have **no automatic expiry**.
No raw microphone audio is stored by this feature. Only signed-in accounts receive durable
memory; anonymous visitors do not share a global memory identity. New tables start empty;
old expired Redis conversations cannot be recovered.

Apply `ai-service/src/main/resources/db/upgrade/20261024_ai_conversation_memory.sql`
with PostgreSQL before deploying to another database. It is additive and rerunnable.
This migration has been applied to the local Docker AI database.

## Retrieval algorithm

- Use the authenticated account ID, never a client-supplied owner, for all model retrieval.
- Keep ADMIN and CUSTOMER histories separate, including when the account is the same.
- Load the last eight turns for a resumed conversation; resume ownership survives Redis expiry.
- For cross-conversation recall, rank case-normalized text using PostgreSQL trigram
  similarity and word similarity over at most 200 matching candidates, with newest ID breaking ties. Short follow-ups also
  consider recent account history. Return at most four snippets of 1,000 characters.
- Chat supplies these snippets as explicitly untrusted user content, separate from
  the system instructions. Calls start with recent same-conversation text and expose
  `recall_conversation(query)` for relevant earlier conversations.
- Save finalized call turns and partial turns on interruption/end. Stable session/turn/role
  IDs make network retries idempotent. A 60-second transcript-only submission grace
  allows the last turn to save after the call deadline; tool access and provider leases
  still expire at the original deadline. The browser retries a failed save once and
  shows an error if it cannot save. Abrupt browser/network loss can lose an unfinished turn.

Memory is historical text, **not model weight training** or approved shop knowledge.
It does not authorize actions or prove current stock, prices, medical claims, permissions,
orders or payments. Existing live service tools and explicit confirmation still apply.

## Admin control

These authenticated endpoints check current administrator status on every request and
return `Cache-Control: no-store`:

- `GET /api/ai/admin/memory?page=0`: 50 conversation metadata records per page.
- `GET /api/ai/admin/memory/{id}?actorId=123&scope=CUSTOMER&page=0`:
  100 saved turns per page. Use the owner ID and scope returned by the list.
- `DELETE /api/ai/admin/memory/{id}?actorId=123&scope=CUSTOMER&confirm=true`:
  explicitly delete that conversation and its saved turns. Without `confirm=true`, reject.

Deletion immediately removes server-side retrieval context. A running Gemini call or
already displayed browser history may still contain text sent before deletion; end that
call and start a new conversation to clear that active context. This feature does not
provide a new admin management screen; management is available through these APIs.

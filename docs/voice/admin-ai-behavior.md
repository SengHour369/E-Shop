# Admin AI behavior improvements

These changes improve routing, instructions and conversation references. They do not fine-tune or retrain model weights, change the configured model, enable billing or grant new permissions.

- Added exact English/Khmer read-request handling for admin orders, payments, returns, accounts, low stock, order counts, capability discovery and admin-service status. It only selects tools supplied by the backend. Explicit TODAY/status parameters are extracted; missing IDs, unsupported ranges and writes are never guessed.
- Added routing instructions for short follow-ups, admin versus personal scope, counts versus payment revenue and data pasted as product notes. Packaging is distinct from stock, and product descriptions do not establish medical benefits.
- Improved the local model's first routing step, which previously omitted most admin-specific guidance.
- Expanded the bounded inference catalog from 40 to 64 tools to accommodate the authorized admin registry. Uniqueness, schema validation and backend permissions still apply.
- Successful admin reads remember their last read-tool name in actor-scoped Redis context. Private record bodies are not copied into that context. Existing selected product/SKU/order references remain available. Missing historical filters still require clarification; an absent filter must not be guessed.
- The admin conversational fallback now uses prior conversation more clearly, recognizes product notes, requests essential missing fields and avoids generic customer-shopping replies.

Validation: the complete inference suite passed 125 tests, including 20 new bilingual/security/catalog-bound examples. Existing local/Gemini provider, conversation and schema tests passed. Focused Java context/admin-access tests also passed; the Redis context tests use isolated namespaces. Four deployed localhost inference requests correctly selected Khmer today-orders, English today-payments, Khmer customer-list and English pending-orders operations. Those checks test routing, not live business records or writes. No paid model call, fine-tuning run or commerce mutation was performed.

Example requests:

- `សូមបង្ហាញការបញ្ជាទិញថ្ងៃនេះ`
- `show payments today`
- `បង្ហាញអតិថិជន`
- `show orders status pending`

The AI still needs an actual permitted tool for an operation. Complex multi-step planning, long-pasted-document extraction and human conversation quality require further real-world evaluation; these tests do not establish human-level understanding or guarantee every phrasing works.

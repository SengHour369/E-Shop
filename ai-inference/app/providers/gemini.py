"""Gemini GenerateContent API: fixed Google endpoint, header authentication, no retries."""
from __future__ import annotations

import json
import httpx

from app.core.errors import AI_INVALID_RESPONSE, AI_PROVIDER_ERROR, AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, AiRouteError
from app.core.request_context import current_traceparent
from app.providers.base import AiProvider
from app.providers.openai import response_schema, decision_from_output, candidates_from_output
from app.schemas.tool_definition import ToolDefinition

_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/"
_MAX_RESPONSE_BYTES = 65536


class GeminiProvider(AiProvider):
    def __init__(self, *, api_key: str, model: str, http: httpx.AsyncClient, timeout_seconds: float) -> None:
        self.api_key = api_key
        self.model = model
        self.http = http
        self.timeout = httpx.Timeout(timeout_seconds, connect=3.0)

    async def _generate(self, instructions: str, contents: list[dict], schema: dict | None = None) -> str:
        if not self.api_key or not self.model:
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 503)
        config: dict = {"maxOutputTokens": 2048, "temperature": 0.2}
        if self.model.startswith("gemini-2.5-flash"):
            config["thinkingConfig"] = {"thinkingBudget": 0}
        if schema is not None:
            config.update(responseMimeType="application/json", responseJsonSchema=schema)
        headers = {"x-goog-api-key": self.api_key, "Content-Type": "application/json", "Accept": "application/json"}
        if current_traceparent():
            headers["traceparent"] = current_traceparent()
        try:
            async with self.http.stream("POST", _ENDPOINT + self.model + ":generateContent", headers=headers,
                    json={"systemInstruction": {"parts": [{"text": instructions}]}, "contents": contents, "generationConfig": config}, timeout=self.timeout) as response:
                if response.status_code in (408, 504):
                    raise AiRouteError(AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, 504)
                if response.status_code != 200:
                    # Never return or log upstream bodies, headers or keys.
                    raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502)
                buffer = bytearray()
                async for chunk in response.aiter_bytes():
                    buffer.extend(chunk)
                    if len(buffer) > _MAX_RESPONSE_BYTES:
                        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)
            payload = json.loads(buffer)
            candidates = payload.get("candidates") if isinstance(payload, dict) else None
            if not isinstance(candidates, list) or not candidates or not isinstance(candidates[0], dict):
                raise ValueError()
            candidate = candidates[0]
            if candidate.get("finishReason") != "STOP":
                raise ValueError()
            parts = candidate.get("content", {}).get("parts", [])
            answer = "".join(p["text"] for p in parts if isinstance(p, dict) and isinstance(p.get("text"), str) and not p.get("thought"))
            if not answer.strip():
                raise ValueError()
            return answer
        except httpx.TimeoutException:
            raise AiRouteError(AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, 504) from None
        except httpx.HTTPError:
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502) from None
        except (ValueError, TypeError, AttributeError):
            raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502) from None

    async def route(self, prompt: str, *, message: str, tools: list[ToolDefinition]) -> dict:
        text = await self._generate(prompt, [{"role": "user", "parts": [{"text": message}]}], response_schema(tools))
        return decision_from_output(text)

    async def identify_products(self, *, media_type: str, image_base64: str) -> dict:
        schema = {"type": "object", "required": ["candidates"], "properties": {
            "candidates": {"type": "array", "maxItems": 5, "items": {"type": "object", "required": ["name", "confidence"],
                "properties": {"name": {"type": "string"}, "confidence": {"type": "number"}}}}}}
        text = await self._generate("Identify at most five retail product names and confidence from zero to one. Never invent prices, catalog IDs, SKUs or barcodes. Return no candidates when unrecognizable.",
            [{"role": "user", "parts": [{"text": "Identify the product in this image."}, {"inlineData": {"mimeType": media_type, "data": image_base64}}]}], schema)
        return candidates_from_output(text)

    async def converse(self, instructions: str, message: str, history: list[dict]) -> str:
        contents = [{"role": "model" if turn["role"] == "assistant" else "user", "parts": [{"text": turn["content"]}]} for turn in history]
        contents.append({"role": "user", "parts": [{"text": message}]})
        return await self._generate(instructions, contents)

"""Local intent inference. The configured server is never selected by chat text."""

from __future__ import annotations

import json

import httpx

from app.core.errors import AI_INVALID_RESPONSE, AI_PROVIDER_ERROR, PUBLIC_MESSAGE, AiRouteError
from app.providers.base import AiProvider
from app.providers.openai import response_schema
from app.schemas.tool_definition import ToolDefinition


class OllamaProvider(AiProvider):
    def __init__(self, *, model: str, base_url: str, http: httpx.AsyncClient) -> None:
        self.model = model
        self.base_url = base_url.rstrip("/")
        self.http = http

    async def route(self, prompt: str, *, message: str, tools: list[ToolDefinition]) -> dict:
        response = await self.http.post(
            self.base_url + "/api/chat",
            json={
                "model": self.model,
                "stream": False,
                "messages": [
                    {"role": "system", "content": prompt},
                    {"role": "user", "content": message},
                ],
                "format": response_schema(tools),
                "options": {"temperature": 0, "num_predict": 1000},
            },
        )
        if response.status_code != 200 or len(response.content) > 65536:
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502)
        try:
            envelope = response.json()
            if envelope.get("done") is not True:
                raise ValueError("Incomplete response")
            result = json.loads(envelope["message"]["content"])
            if not isinstance(result, dict):
                raise ValueError("Expected object")
            result["tool"] = None if result.get("intent") == "UNKNOWN" else result.get("intent")
            return result
        except (ValueError, KeyError, TypeError, AttributeError):
            raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502) from None

    async def identify_products(self, *, media_type: str, image_base64: str) -> dict:
        # Selecting a chat-only local model must not silently produce visual guesses.
        raise AiRouteError(AI_PROVIDER_ERROR, "Vision is unavailable for this provider", 503)

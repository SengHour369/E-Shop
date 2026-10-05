"""OpenAI Responses API. The endpoint, model, and schema are fixed. There are no retries."""

from __future__ import annotations

import json
from typing import Any

import httpx

from app.core.errors import (
    AI_INVALID_RESPONSE,
    AI_PROVIDER_ERROR,
    AI_PROVIDER_TIMEOUT,
    PUBLIC_MESSAGE,
    AiRouteError,
)
from app.core.request_context import current_traceparent
from app.providers.base import AiProvider
from app.schemas.tool_definition import ToolDefinition

_MAX_RESPONSE_CHARS = 65536
_MAX_OUTPUT_TOKENS = 1000
_JSON_TYPES = {
    "boolean": "boolean",
    "integer": "integer",
    "number": "number",
    "string": "string",
}


class OpenAiProvider(AiProvider):
    def __init__(
        self,
        *,
        api_key: str,
        model: str,
        base_url: str,
        http: httpx.AsyncClient,
        timeout_seconds: float,
    ) -> None:
        self.api_key = api_key
        self.model = model
        self.base_url = base_url.rstrip("/")
        self.http = http
        self.timeout = httpx.Timeout(timeout_seconds, connect=3.0)

    async def route(self, prompt: str, *, message: str, tools: list[ToolDefinition]) -> dict:
        if not self.api_key:
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 503)
        headers = {
            "Authorization": "Bearer " + self.api_key,
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        parent = current_traceparent()
        if parent:
            headers["traceparent"] = parent
        try:
            response = await self.http.post(
                self.base_url + "/responses",
                json=_request_body(self.model, prompt, message, tools),
                headers=headers,
                timeout=self.timeout,
            )
        except httpx.TimeoutException:
            raise AiRouteError(AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, 504) from None
        except httpx.HTTPError:
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502) from None
        return _read_response(response)

    async def identify_products(self, *, media_type: str, image_base64: str) -> dict:
        if not self.api_key:
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 503)
        headers = {
            "Authorization": "Bearer " + self.api_key,
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        parent = current_traceparent()
        if parent:
            headers["traceparent"] = parent
        try:
            response = await self.http.post(
                self.base_url + "/responses",
                json=_vision_body(self.model, media_type, image_base64),
                headers=headers,
                timeout=self.timeout,
            )
        except httpx.TimeoutException:
            raise AiRouteError(AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, 504) from None
        except httpx.HTTPError:
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502) from None
        return candidates_from_output(_output_text(response))


def response_schema(tools: list[ToolDefinition]) -> dict[str, Any]:
    """Strict schema. Parameter names come only from the supplied tools."""
    parameters: dict[str, Any] = {}
    for tool in tools:
        for name, type_name in tool.parameters.items():
            if name not in parameters:
                parameters[name] = {"type": [_JSON_TYPES[type_name], "null"]}
    intents = sorted({tool.name for tool in tools} | {"UNKNOWN"})
    return {
        "type": "object",
        "additionalProperties": False,
        "required": ["intent", "confidence", "parameters"],
        "properties": {
            "intent": {"type": "string", "enum": intents},
            "confidence": {"type": "number"},
            "parameters": {
                "type": "object",
                "additionalProperties": False,
                "properties": parameters,
                "required": sorted(parameters),
            },
        },
    }


def _request_body(model: str, prompt: str, message: str, tools: list[ToolDefinition]) -> dict[str, Any]:
    return {
        "model": model,
        "store": False,
        "max_output_tokens": _MAX_OUTPUT_TOKENS,
        "instructions": prompt,
        "input": message,
        "text": {
            "format": {
                "type": "json_schema",
                "name": "eshop_intent",
                "strict": True,
                "schema": response_schema(tools),
            }
        },
    }


_VISION_INSTRUCTIONS = (
    "Identify the retail product in the photo. Return at most 5 readable product names "
    "and a confidence from 0 to 1. Do not invent barcodes, prices, SKUs, or catalog ids. "
    "If nothing is recognizable, return an empty candidate list."
)

_VISION_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["candidates"],
    "properties": {
        "candidates": {
            "type": "array",
            "maxItems": 5,
            "items": {
                "type": "object",
                "additionalProperties": False,
                "required": ["name", "confidence"],
                "properties": {
                    "name": {"type": "string"},
                    "confidence": {"type": "number"},
                },
            },
        }
    },
}


def _vision_body(model: str, media_type: str, image_base64: str) -> dict[str, Any]:
    return {
        "model": model,
        "store": False,
        "max_output_tokens": 400,
        "instructions": _VISION_INSTRUCTIONS,
        "input": [{
            "role": "user",
            "content": [
                {"type": "input_text", "text": "Name the product shown in this image."},
                {"type": "input_image", "image_url": f"data:{media_type};base64,{image_base64}"},
            ],
        }],
        "text": {
            "format": {
                "type": "json_schema",
                "name": "product_candidates",
                "strict": True,
                "schema": _VISION_SCHEMA,
            }
        },
    }


def _output_text(response: httpx.Response) -> str:
    if response.status_code in (408, 504):
        raise AiRouteError(AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, 504)
    if response.status_code != 200 or len(response.content) > _MAX_RESPONSE_CHARS:
        raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502)
    try:
        payload = response.json()
    except ValueError:
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502) from None
    if not isinstance(payload, dict) or payload.get("status") != "completed":
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)
    for item in payload.get("output") or []:
        if not isinstance(item, dict):
            continue
        for part in item.get("content") or []:
            if isinstance(part, dict) and part.get("type") == "output_text" and isinstance(part.get("text"), str):
                return part["text"]
    raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)


def candidates_from_output(text: str) -> dict:
    try:
        raw = json.loads(text)
    except ValueError:
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502) from None
    rows = raw.get("candidates") if isinstance(raw, dict) else None
    if not isinstance(rows, list):
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)
    candidates = []
    for row in rows[:5]:
        if not isinstance(row, dict) or not isinstance(row.get("name"), str):
            continue
        confidence = row.get("confidence")
        if not isinstance(confidence, (int, float)):
            continue
        candidates.append({"name": row["name"].strip(), "confidence": float(confidence)})
    return {"candidates": candidates}


def _read_response(response: httpx.Response) -> dict:
    if response.status_code in (408, 504):
        raise AiRouteError(AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, 504)
    if response.status_code != 200 or len(response.content) > _MAX_RESPONSE_CHARS:
        raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502)
    try:
        payload = response.json()
    except ValueError:
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502) from None
    if not isinstance(payload, dict) or payload.get("status") != "completed":
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)
    for item in payload.get("output") or []:
        if not isinstance(item, dict):
            continue
        for part in item.get("content") or []:
            if isinstance(part, dict) and part.get("type") == "output_text":
                return decision_from_output(part.get("text"))
    raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)


def decision_from_output(text: Any) -> dict:
    if not isinstance(text, str):
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)
    try:
        raw = json.loads(text)
    except ValueError:
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502) from None
    if not isinstance(raw, dict):
        raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)
    if "tool" not in raw and isinstance(raw.get("intent"), str):
        raw = dict(raw)
        raw["tool"] = None if raw["intent"] == "UNKNOWN" else raw["intent"]
    return raw

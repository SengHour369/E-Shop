"""Test helpers. Nothing in this module calls a paid provider."""

from __future__ import annotations

from app.core.config import Settings
from app.providers.base import AiProvider
from app.schemas.tool_definition import ToolDefinition


def make_settings(**overrides) -> Settings:
    values = {
        "ai_provider": "OPENAI",
        "ai_request_timeout_seconds": 20,
        "ai_min_confidence": 0.70,
        "openai_api_key": "sk-test",
        "openai_model": "gpt-4.1-mini",
        "openai_base_url": "https://api.openai.com/v1",
        "log_level": "INFO",
    }
    values.update(overrides)
    return Settings(**values, _env_file=None)


def tool(name: str, description: str, **parameters: str) -> ToolDefinition:
    return ToolDefinition(name=name, description=description, parameters=parameters)


class FakeProvider(AiProvider):
    def __init__(self) -> None:
        self.decisions: list[object] = []
        self.prompts: list[str] = []
        self.messages: list[str] = []
        self.calls = 0
        self.visions: list[object] = []

    def push(self, decision: object) -> None:
        self.decisions.append(decision)

    def push_vision(self, decision: object) -> None:
        self.visions.append(decision)

    async def identify_products(self, *, media_type: str, image_base64: str) -> dict:
        self.calls += 1
        if not self.visions:
            raise AssertionError("no fake vision result was queued")
        item = self.visions.pop(0)
        if isinstance(item, Exception):
            raise item
        return item

    async def route(self, prompt: str, *, message: str, tools: list[ToolDefinition]) -> dict:
        self.calls += 1
        self.prompts.append(prompt)
        self.messages.append(message)
        if not self.decisions:
            raise AssertionError("no fake decision was queued")
        item = self.decisions.pop(0)
        if isinstance(item, Exception):
            raise item
        return item

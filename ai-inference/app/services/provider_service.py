"""Select the configured provider, enforce the timeout, and validate its JSON."""

from __future__ import annotations

import asyncio
import logging

import httpx
from pydantic import ValidationError

from app.core.config import Settings, readiness
from app.core.errors import (
    AI_INVALID_RESPONSE,
    AI_PROVIDER_ERROR,
    AI_PROVIDER_TIMEOUT,
    PUBLIC_MESSAGE,
    AiRouteError,
)
from app.providers.base import AiProvider
from app.schemas.ai_response import ModelDecision
from app.schemas.tool_definition import ToolDefinition

log = logging.getLogger(__name__)


class AiProviderService:
    def __init__(self, provider: AiProvider, settings: Settings) -> None:
        self.provider = provider
        self.settings = settings

    async def route(self, instructions: str, message: str, tools: list[ToolDefinition]) -> ModelDecision:
        if not readiness(self.settings):
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 503)
        try:
            raw = await asyncio.wait_for(
                self.provider.route(instructions, message=message, tools=tools),
                timeout=self.settings.ai_request_timeout_seconds + 1,
            )
        except AiRouteError:
            raise
        except (TimeoutError, httpx.TimeoutException):
            log.warning("AI provider timed out provider=%s", self.settings.ai_provider)
            raise AiRouteError(AI_PROVIDER_TIMEOUT, PUBLIC_MESSAGE, 504) from None
        except Exception as ex:
            log.warning(
                "AI provider request failed provider=%s errorType=%s",
                self.settings.ai_provider,
                type(ex).__name__,
            )
            raise AiRouteError(AI_PROVIDER_ERROR, PUBLIC_MESSAGE, 502) from None
        return self.parse(raw)

    @staticmethod
    def parse(raw: object) -> ModelDecision:
        if not isinstance(raw, dict):
            raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502)
        try:
            return ModelDecision.model_validate(raw)
        except ValidationError:
            raise AiRouteError(AI_INVALID_RESPONSE, PUBLIC_MESSAGE, 502) from None

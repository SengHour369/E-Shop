"""Turn a user message into one allowed tool decision. Untrusted output becomes UNKNOWN."""

from __future__ import annotations

import logging
import time
from typing import Any

from app.core.config import Settings
from app.core.errors import (
    AI_INVALID_PARAMETERS,
    AI_TOOL_NOT_ALLOWED,
    AI_UNKNOWN_INTENT,
    DecisionRejected,
)
from app.schemas.ai_request import AiRouteRequest
from app.schemas.ai_response import AiRouteResponse, ModelDecision, unknown_response
from app.services.prompt_service import build_instructions
from app.services.provider_service import AiProviderService
from app.tools.allowed_tools import requires_confirmation, value_matches
from app.tools.registry import index_tools

log = logging.getLogger(__name__)


class IntentService:
    def __init__(self, settings: Settings, providers: AiProviderService) -> None:
        self.settings = settings
        self.providers = providers

    async def route(self, request: AiRouteRequest) -> AiRouteResponse:
        started = time.perf_counter()
        log.info("AI routing started provider=%s", self.settings.ai_provider)
        instructions = build_instructions(request.tools)
        try:
            decision = await self.providers.route(instructions, request.message, request.tools)
            result = self.accept(request, decision)
        except DecisionRejected as rejected:
            log.info("AI decision rejected code=%s", rejected.code)
            result = unknown_response()
            self._log_result(result, started, rejected.code)
            return result
        self._log_result(result, started, "success")
        return result

    def accept(self, request: AiRouteRequest, decision: ModelDecision) -> AiRouteResponse:
        tools = index_tools(request.tools)
        if decision.intent == "UNKNOWN" or decision.tool is None:
            if decision.intent != "UNKNOWN" or decision.tool is not None:
                raise DecisionRejected(AI_UNKNOWN_INTENT)
            self._require_confidence(decision.confidence, allow_low=True)
            return unknown_response(decision.confidence)

        if decision.intent != decision.tool or decision.tool not in tools:
            raise DecisionRejected(AI_TOOL_NOT_ALLOWED)
        self._require_confidence(decision.confidence, allow_low=False)
        parameters = _matching_parameters(tools[decision.tool].parameters, decision.parameters)
        return AiRouteResponse(
            intent=decision.tool,
            tool=decision.tool,
            confidence=decision.confidence,
            parameters=parameters,
            requires_confirmation=requires_confirmation(decision.tool),
        )

    def _require_confidence(self, confidence: float, *, allow_low: bool) -> None:
        if confidence < 0 or confidence > 1:
            raise DecisionRejected(AI_UNKNOWN_INTENT)
        if not allow_low and confidence < self.settings.ai_min_confidence:
            raise DecisionRejected(AI_UNKNOWN_INTENT)

    def _log_result(self, result: AiRouteResponse, started: float, outcome: str) -> None:
        elapsed_ms = int((time.perf_counter() - started) * 1000)
        log.info(
            "tool=%s confidence=%s durationMs=%s outcome=%s provider=%s",
            result.tool or "-",
            result.confidence,
            elapsed_ms,
            outcome,
            self.settings.ai_provider,
        )


def _matching_parameters(schema: dict[str, str], supplied: dict[str, Any]) -> dict[str, Any]:
    present = {key: value for key, value in supplied.items() if value is not None}
    cleaned: dict[str, Any] = {}
    for key, value in present.items():
        expected = schema.get(key)
        if expected is None or not value_matches(expected, value):
            raise DecisionRejected(AI_INVALID_PARAMETERS)
        cleaned[key] = value
    return cleaned

"""Public route response and the stricter shape accepted from a provider."""

from __future__ import annotations

import math
from typing import Any

from pydantic import AliasChoices, BaseModel, ConfigDict, Field, field_validator


class AiRouteResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    intent: str
    tool: str | None
    confidence: float = Field(ge=0, le=1)
    parameters: dict[str, Any] = Field(default_factory=dict)
    requires_confirmation: bool = Field(
        default=False,
        validation_alias=AliasChoices("requiresConfirmation", "requires_confirmation"),
        serialization_alias="requiresConfirmation",
    )


class ErrorResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    code: str
    message: str
    request_id: str = Field(serialization_alias="requestId")


class ModelDecision(BaseModel):
    """Parsed model JSON. Unexpected fields are rejected rather than ignored."""

    model_config = ConfigDict(extra="forbid")

    intent: str = Field(min_length=1, max_length=64)
    tool: str | None = None
    confidence: float
    parameters: dict[str, Any] = Field(default_factory=dict)

    @field_validator("confidence", mode="before")
    @classmethod
    def finite_confidence(cls, value: Any) -> float:
        if isinstance(value, bool) or not isinstance(value, (int, float)):
            raise ValueError("confidence must be a number")
        number = float(value)
        if not math.isfinite(number):
            raise ValueError("confidence must be finite")
        return number

    @field_validator("tool", mode="before")
    @classmethod
    def blank_tool_is_absent(cls, value: Any) -> Any:
        if value == "":
            return None
        return value


def unknown_response(confidence: float = 0) -> AiRouteResponse:
    if not math.isfinite(confidence) or confidence < 0 or confidence > 1:
        confidence = 0
    return AiRouteResponse(
        intent="UNKNOWN",
        tool=None,
        confidence=confidence,
        parameters={},
        requires_confirmation=False,
    )

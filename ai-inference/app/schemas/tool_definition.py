"""Tools supplied by Spring Boot for this request. The model cannot add to this list."""

from __future__ import annotations

import re

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.tools.allowed_tools import ALLOWED_PARAMETER_TYPES

_TOOL_NAME = re.compile(r"[A-Z][A-Z0-9_]{0,63}")
_PARAMETER_NAME = re.compile(r"[A-Za-z][A-Za-z0-9_]{0,63}")


class ToolDefinition(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    name: str = Field(min_length=1, max_length=64)
    description: str = Field(min_length=1, max_length=1000)
    parameters: dict[str, str] = Field(default_factory=dict)

    @field_validator("name")
    @classmethod
    def reserved_and_shaped(cls, value: str) -> str:
        if value == "UNKNOWN" or _TOOL_NAME.fullmatch(value) is None:
            raise ValueError("invalid tool name")
        return value

    @field_validator("parameters")
    @classmethod
    def known_parameter_types(cls, value: dict[str, str]) -> dict[str, str]:
        if len(value) > 30:
            raise ValueError("too many parameters")
        for name, type_name in value.items():
            if _PARAMETER_NAME.fullmatch(name) is None:
                raise ValueError("invalid parameter name")
            if type_name not in ALLOWED_PARAMETER_TYPES:
                raise ValueError("unsupported parameter type")
        return value

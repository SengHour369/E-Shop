"""Index the tools Spring Boot supplied on this request."""

from __future__ import annotations

from app.core.errors import AI_INVALID_PARAMETERS, DecisionRejected
from app.schemas.tool_definition import ToolDefinition


def index_tools(tools: list[ToolDefinition]) -> dict[str, ToolDefinition]:
    indexed: dict[str, ToolDefinition] = {}
    for tool in tools:
        if tool.name in indexed:
            raise DecisionRejected(AI_INVALID_PARAMETERS)
        indexed[tool.name] = tool
    return indexed

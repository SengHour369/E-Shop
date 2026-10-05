"""Provider boundary. Implementations return model JSON and do not call business services."""

from abc import ABC, abstractmethod

from app.schemas.tool_definition import ToolDefinition


class AiProvider(ABC):
    @abstractmethod
    async def route(self, prompt: str, *, message: str, tools: list[ToolDefinition]) -> dict:
        """`prompt` is the system instructions. `message` is untrusted and stays separate."""

    @abstractmethod
    async def identify_products(self, *, media_type: str, image_base64: str) -> dict:
        """Return product-name suggestions. Do not return catalog ids or prices."""

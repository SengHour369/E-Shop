import json

import httpx
import pytest

from app.core.config import Settings, readiness
from app.providers.ollama import OllamaProvider
from app.schemas.tool_definition import ToolDefinition


@pytest.mark.asyncio
async def test_local_provider_separates_user_text_and_returns_valid_decision():
    def handler(request):
        body = json.loads(request.content)
        assert request.url.path == "/api/chat"
        assert body["stream"] is False
        assert body["messages"][0] == {"role": "system", "content": "fixed rules"}
        assert body["messages"][1] == {"role": "user", "content": "បង្ហាញ shoes"}
        return httpx.Response(200, json={
            "done": True,
            "message": {"content": json.dumps({
                "intent": "PRODUCT_SEARCH",
                "confidence": 0.95,
                "parameters": {"query": "shoes"},
            })},
        })

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as http:
        provider = OllamaProvider(model="test", base_url="http://localhost:11434", http=http)
        result = await provider.route(
            "fixed rules",
            message="បង្ហាញ shoes",
            tools=[ToolDefinition(name="PRODUCT_SEARCH", description="Search", parameters={"query": "string"})],
        )
        assert result["tool"] == "PRODUCT_SEARCH"


def test_local_provider_requires_explicit_model():
    assert not readiness(Settings(ai_provider="OLLAMA", ollama_model=""))
    assert readiness(Settings(ai_provider="OLLAMA", ollama_model="test-model"))

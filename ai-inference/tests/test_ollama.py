import json

import httpx
import pytest

from app.core.config import Settings, readiness
from app.providers.ollama import OllamaProvider
from app.schemas.tool_definition import ToolDefinition


@pytest.mark.asyncio
async def test_local_provider_separates_user_text_and_returns_valid_decision():
    requests = []
    def handler(request):
        body = json.loads(request.content)
        assert request.url.path == "/api/chat"
        assert body["stream"] is False
        assert body["messages"][0]["role"] == "system"
        assert "បង្ហាញ shoes" not in body["messages"][0]["content"]
        requests.append(body)
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
        assert result["parameters"] == {"query": "shoes"}
        assert len(requests) == 2
        assert "parameters" not in requests[0]["format"]["properties"]
        assert set(requests[1]["format"]["properties"]["parameters"]["properties"]) == {"query"}


def test_local_provider_requires_explicit_model():
    assert not readiness(Settings(ai_provider="OLLAMA", ollama_model=""))
    assert readiness(Settings(ai_provider="OLLAMA", ollama_model="test-model"))


def test_routing_schema_does_not_force_unrelated_parameters_into_read_requests():
    from app.providers.ollama import routing_schema
    schema = routing_schema([
        ToolDefinition(name="PRODUCT_SEARCH", description="Search", parameters={"query": "string"}),
        ToolDefinition(name="MY_ORDERS", description="My orders", parameters={}),
        ToolDefinition(name="PROMOTION_CREATE", description="Draft", parameters={"skuId": "integer", "discount": "number"}),
    ])
    parameters = schema["properties"]["parameters"]
    assert not parameters.get("required")
    assert parameters["additionalProperties"] is False
    assert set(parameters["properties"]) == {"query", "skuId", "discount"}
    assert set(schema["properties"]["intent"]["enum"]) == {"PRODUCT_SEARCH", "MY_ORDERS", "PROMOTION_CREATE", "UNKNOWN"}


@pytest.mark.asyncio
async def test_parameterless_order_list_needs_no_argument_generation():
    calls = []
    def handler(request):
        calls.append(json.loads(request.content))
        return httpx.Response(200, json={"done": True, "message": {"content": json.dumps({"intent": "MY_ORDERS", "confidence": 0.95})}})
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as http:
        result = await OllamaProvider(model="test", base_url="http://localhost:11434", http=http).route(
            "fixed", message="Show my orders", tools=[ToolDefinition(name="MY_ORDERS", description="List my orders", parameters={})])
    assert result == {"intent": "MY_ORDERS", "tool": "MY_ORDERS", "confidence": 0.95, "parameters": {}}
    assert len(calls) == 1

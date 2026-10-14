import json
import httpx
import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from app.providers import build_provider
from app.providers.gemini import GeminiProvider
from app.core.config import readiness
from app.core.errors import AiRouteError
from app.schemas.tool_definition import ToolDefinition
from tests.support import make_settings

KEY = "test-gemini-key"


def output(text, finish="STOP"):
    return {"candidates": [{"finishReason": finish, "content": {"parts": [{"text": text}]}}]}


def provider(http, key=KEY):
    return GeminiProvider(api_key=key, model="gemini-3.8-flash", http=http, timeout_seconds=20)


@pytest.mark.asyncio
async def test_structured_routing_keeps_key_out_of_url_and_user_text_out_of_system():
    requests = []
    def handle(request):
        requests.append(request)
        return httpx.Response(200, json=output(json.dumps({"intent": "PRODUCT_SEARCH", "confidence": 0.99, "parameters": {"query": "headphones"}})))
    async with httpx.AsyncClient(transport=httpx.MockTransport(handle)) as http:
        result = await provider(http).route("Fixed system rules", message="Find headphones", tools=[ToolDefinition(name="PRODUCT_SEARCH", description="Search", parameters={"query": "string"})])
    request = requests[0]; body = json.loads(request.content)
    assert request.headers["x-goog-api-key"] == KEY and KEY not in str(request.url)
    assert request.url.host == "generativelanguage.googleapis.com" and not request.url.query
    assert body["systemInstruction"]["parts"] == [{"text": "Fixed system rules"}]
    assert body["contents"][0]["parts"] == [{"text": "Find headphones"}]
    assert body["generationConfig"]["responseJsonSchema"]["properties"]["intent"]["enum"] == ["PRODUCT_SEARCH", "UNKNOWN"]
    assert result["tool"] == "PRODUCT_SEARCH"


@pytest.mark.asyncio
@pytest.mark.parametrize("status,code", [(401,"AI_PROVIDER_ERROR"), (429,"AI_PROVIDER_ERROR"), (500,"AI_PROVIDER_ERROR"), (504,"AI_PROVIDER_TIMEOUT")])
async def test_upstream_errors_never_expose_body_or_key(status, code):
    async with httpx.AsyncClient(transport=httpx.MockTransport(lambda request: httpx.Response(status, text=KEY))) as http:
        with pytest.raises(AiRouteError) as caught:
            await provider(http).route("Rules",message="Hello",tools=[])
    assert caught.value.code == code and KEY not in str(caught.value)


@pytest.mark.asyncio
@pytest.mark.parametrize("payload", [output("{}", "MAX_TOKENS"), {"promptFeedback": {"blockReason": "SAFETY"}}, output("not JSON"), {"candidates": [None]}])
async def test_blocked_partial_or_malformed_responses_fail_closed(payload):
    async with httpx.AsyncClient(transport=httpx.MockTransport(lambda request: httpx.Response(200,json=payload))) as http:
        with pytest.raises(AiRouteError) as caught:
            await provider(http).route("Rules",message="Hello",tools=[])
    assert caught.value.code == "AI_INVALID_RESPONSE"


@pytest.mark.asyncio
async def test_response_size_limit_and_transport_timeout():
    async with httpx.AsyncClient(transport=httpx.MockTransport(lambda request: httpx.Response(200,content=b"x"*65537))) as http:
        with pytest.raises(AiRouteError) as caught:
            await provider(http).route("Rules",message="Hello",tools=[])
        assert caught.value.code == "AI_INVALID_RESPONSE"
    def timeout(request):
        raise httpx.ReadTimeout("secret upstream details",request=request)
    async with httpx.AsyncClient(transport=httpx.MockTransport(timeout)) as http:
        with pytest.raises(AiRouteError) as caught:
            await provider(http).route("Rules",message="Hello",tools=[])
        assert caught.value.code == "AI_PROVIDER_TIMEOUT"


@pytest.mark.asyncio
async def test_vision_uses_inline_image_and_returns_only_suggestions():
    bodies = []
    def handle(request):
        bodies.append(json.loads(request.content))
        return httpx.Response(200,json=output(json.dumps({"candidates":[{"name":"Headphones","confidence":0.9,"catalogId":1}]})))
    async with httpx.AsyncClient(transport=httpx.MockTransport(handle)) as http:
        result = await provider(http).identify_products(media_type="image/png",image_base64="aW1hZ2U=")
    assert bodies[0]["contents"][0]["parts"][1]["inlineData"] == {"mimeType":"image/png","data":"aW1hZ2U="}
    assert result == {"candidates":[{"name":"Headphones","confidence":0.9}]}


def test_gemini_conversation_history_and_safe_model_info():
    bodies=[]
    def handle(request):
        bodies.append(json.loads(request.content))
        return httpx.Response(200,json=output("Hello!"))
    settings=make_settings(ai_provider="GEMINI",gemini_api_key=KEY,gemini_model="gemini-3.8-flash")
    app=create_app(settings)
    with TestClient(app) as client:
        app.state.http=httpx.AsyncClient(transport=httpx.MockTransport(handle))
        result=client.post("/api/v1/ai/conversation",json={"message":"Hello","history":[{"role":"assistant","content":"Welcome"}]})
        assert result.status_code == 200 and result.json()["message"] == "Hello!"
        info=client.get("/api/v1/ai/info")
        assert info.json()["model"] == settings.gemini_model and KEY not in info.text
    assert bodies[0]["contents"][0] == {"role":"model","parts":[{"text":"Welcome"}]}
    assert "responseJsonSchema" not in bodies[0]["generationConfig"] and "tools" not in bodies[0]


def test_configuration_selects_gemini_and_rejects_missing_key():
    settings=make_settings(ai_provider="gemini",gemini_api_key=KEY)
    assert readiness(settings) and KEY not in repr(settings)
    assert not readiness(make_settings(ai_provider="GEMINI",gemini_api_key=""))
    assert not readiness(make_settings(ai_provider="GEMINI",gemini_api_key=KEY,gemini_model=""))
    assert isinstance(build_provider(settings,httpx.AsyncClient()),GeminiProvider)


@pytest.mark.asyncio
async def test_missing_key_does_not_call_google():
    called=[]
    async with httpx.AsyncClient(transport=httpx.MockTransport(lambda request: called.append(request))) as http:
        with pytest.raises(AiRouteError) as caught:
            await provider(http,key="").route("Rules",message="Hello",tools=[])
    assert caught.value.status == 503 and called == []

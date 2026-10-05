import base64

import httpx
import pytest

from app.core.errors import AiRouteError
from app.providers.openai import OpenAiProvider, candidates_from_output
from app.services.vision_service import public_candidates

_JPEG = base64.b64encode(b"\xff\xd8\xff\x00").decode("ascii")


def test_model_product_ids_are_dropped():
    parsed = candidates_from_output(
        '{"candidates":[{"name":"Nike Air Max","confidence":0.91,"productId":100}]}'
    )
    public = public_candidates(parsed)
    assert public[0].name == "Nike Air Max"
    assert not hasattr(public[0], "product_id")
    assert "productId" not in public[0].model_dump()


def test_high_confidence_candidate(client, fake):
    fake.push_vision({"candidates": [{"name": "Nike Air Max", "confidence": 0.95}]})
    response = client.post("/api/v1/ai/vision/products", json={"mediaType": "image/jpeg", "imageBase64": _JPEG},
                           headers={"X-Request-ID": "req_vision"})
    assert response.status_code == 200
    assert response.headers["x-request-id"] == "req_vision"
    assert response.json() == {"candidates": [{"name": "Nike Air Max", "confidence": 0.95}]}


def test_low_confidence_and_multiple_candidates(client, fake):
    fake.push_vision({"candidates": [
        {"name": "Nike Air Max", "confidence": 0.74},
        {"name": "Nike Air Max 90", "confidence": 0.66},
    ]})
    body = client.post("/api/v1/ai/vision/products", json={"mediaType": "image/jpeg", "imageBase64": _JPEG}).json()
    assert [item["confidence"] for item in body["candidates"]] == [0.74, 0.66]
    assert "productId" not in body["candidates"][0]


def test_no_visible_product_returns_an_empty_list(client, fake):
    fake.push_vision({"candidates": []})
    response = client.post("/api/v1/ai/vision/products", json={"mediaType": "image/png", "imageBase64": _JPEG})
    assert response.status_code == 200
    assert response.json()["candidates"] == []


def test_provider_unavailable(client, fake):
    fake.push_vision(AiRouteError("AI_PROVIDER_ERROR", "Product recognition is temporarily unavailable", 503))
    response = client.post("/api/v1/ai/vision/products", json={"mediaType": "image/jpeg", "imageBase64": _JPEG})
    assert response.status_code == 503
    assert response.json()["code"] == "AI_PROVIDER_ERROR"
    assert "sk-" not in response.text


def test_invalid_image_does_not_call_the_provider(client, fake):
    response = client.post("/api/v1/ai/vision/products", json={"mediaType": "image/gif", "imageBase64": _JPEG})
    assert response.status_code == 422
    assert fake.calls == 0


@pytest.mark.asyncio
async def test_openai_vision_request_has_no_catalog_authority():
    seen = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["body"] = request.read().decode("utf-8")
        seen["auth"] = request.headers.get("authorization")
        text = '{"candidates":[{"name":"Nike Air Max","confidence":0.8}]}'
        payload = {"status": "completed", "output": [{"content": [{"type": "output_text", "text": text}]}]}
        return httpx.Response(200, json=payload)

    http = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    provider = OpenAiProvider(api_key="sk-test", model="gpt-4.1-mini", base_url="https://api.openai.com/v1",
                              http=http, timeout_seconds=2)
    result = await provider.identify_products(media_type="image/jpeg", image_base64=_JPEG)
    assert result["candidates"][0]["name"] == "Nike Air Max"
    assert "productId" not in seen["body"]
    assert "input_image" in seen["body"]
    assert seen["auth"] == "Bearer sk-test"
    await http.aclose()

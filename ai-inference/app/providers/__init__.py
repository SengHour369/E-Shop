from app.core.config import Settings
from app.providers.base import AiProvider
from app.providers.openai import OpenAiProvider

import httpx


def build_provider(settings: Settings, http: httpx.AsyncClient) -> AiProvider:
    """The repository uses the OpenAI Responses API. Other names fail closed at call time."""
    return OpenAiProvider(
        api_key=settings.openai_api_key,
        model=settings.openai_model,
        base_url=settings.openai_base_url,
        http=http,
        timeout_seconds=settings.ai_request_timeout_seconds,
    )

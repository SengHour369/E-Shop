from app.core.config import Settings
from app.providers.base import AiProvider
from app.providers.openai import OpenAiProvider
from app.providers.ollama import OllamaProvider

import httpx


def build_provider(settings: Settings, http: httpx.AsyncClient) -> AiProvider:
    """Select only an operator-configured provider. Unknown names fail readiness."""
    if settings.ai_provider == "OLLAMA":
        return OllamaProvider(
            model=settings.ollama_model,
            base_url=settings.ollama_base_url,
            http=http,
        )
    return OpenAiProvider(
        api_key=settings.openai_api_key,
        model=settings.openai_model,
        base_url=settings.openai_base_url,
        http=http,
        timeout_seconds=settings.ai_request_timeout_seconds,
    )

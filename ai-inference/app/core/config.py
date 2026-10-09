"""Environment configuration. API keys are never given a source-code default."""

from __future__ import annotations

import re
from urllib.parse import urlparse

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

_MODEL_NAME = re.compile(r"[A-Za-z0-9._:-]{1,64}")


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore",
        case_sensitive=False,
    )

    ai_provider: str = "OPENAI"
    ollama_model: str = ""
    ollama_base_url: str = "http://localhost:11434"
    ai_request_timeout_seconds: float = Field(default=20, gt=0, le=120)
    ai_min_confidence: float = Field(default=0.70, ge=0, le=1)
    openai_api_key: str = ""
    openai_model: str = "gpt-4.1-mini"
    openai_base_url: str = "https://api.openai.com/v1"
    log_level: str = "INFO"

    @field_validator("ai_provider")
    @classmethod
    def normalize_provider(cls, value: str) -> str:
        normalized = value.strip().upper()
        if not normalized:
            raise ValueError("AI_PROVIDER is required")
        return normalized

    @field_validator("ollama_base_url")
    @classmethod
    def local_provider_url(cls, value: str) -> str:
        parsed = urlparse(value)
        if parsed.scheme not in {"http", "https"} or not parsed.hostname:
            raise ValueError("OLLAMA_BASE_URL must be an HTTP URL")
        if parsed.username or parsed.password or parsed.query or parsed.fragment:
            raise ValueError("OLLAMA_BASE_URL cannot contain credentials, query or fragment")
        return value.rstrip("/")

    @field_validator("openai_api_key")
    @classmethod
    def normalize_key(cls, value: str) -> str:
        cleaned = value.strip()
        if "\r" in cleaned or "\n" in cleaned:
            raise ValueError("OPENAI_API_KEY is invalid")
        return cleaned

    @field_validator("openai_model")
    @classmethod
    def normalize_model(cls, value: str) -> str:
        cleaned = value.strip()
        if cleaned and _MODEL_NAME.fullmatch(cleaned) is None:
            raise ValueError("OPENAI_MODEL is invalid")
        return cleaned

    @field_validator("openai_base_url")
    @classmethod
    def https_provider_url(cls, value: str) -> str:
        cleaned = value.strip().rstrip("/")
        parsed = urlparse(cleaned)
        if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.password:
            raise ValueError("OPENAI base URL must be https and must not contain credentials")
        return cleaned

    @field_validator("log_level")
    @classmethod
    def normalize_level(cls, value: str) -> str:
        return value.strip().upper() or "INFO"


def readiness(settings: Settings) -> bool:
    """Configuration required to serve traffic. This must not call the provider."""
    if settings.ai_provider == "OLLAMA":
        return bool(settings.ollama_model.strip())
    return (
        settings.ai_provider == "OPENAI"
        and bool(settings.openai_api_key)
        and bool(settings.openai_model)
    )

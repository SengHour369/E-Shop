"""FastAPI entrypoint. A reusable HTTP client is created for the process, not per request."""

from __future__ import annotations

import logging
import time
from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from app.api.ai_router import ai_route_error, invalid_request, router
from app.api.vision import router as vision_router
from app.services.vision_service import VisionService
from app.core.config import Settings
from app.core.errors import AiRouteError
from app.core.logging import configure_logging
from app.core.request_context import (
    REQUEST_ID_HEADER,
    TRACEPARENT_HEADER,
    current_request_id,
    reset_request_id,
    reset_traceparent,
    resolve_request_id,
    set_request_id,
    set_traceparent,
    valid_traceparent,
)
from app.providers import build_provider
from app.services.intent_service import IntentService
from app.services.provider_service import AiProviderService
from fastapi.exceptions import RequestValidationError
from starlette.datastructures import Headers

log = logging.getLogger(__name__)


class RequestContextMiddleware:
    """Copy X-Request-ID and traceparent. Generate an id only when none was supplied."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        headers = Headers(scope=scope)
        request_id = resolve_request_id(headers.getlist(REQUEST_ID_HEADER))
        traceparent = headers.get(TRACEPARENT_HEADER)
        if not valid_traceparent(traceparent):
            traceparent = None
        request_token = set_request_id(request_id)
        trace_token = set_traceparent(traceparent)
        started = time.perf_counter()
        status_holder = {"status": 500}

        async def send_with_context(message: Message) -> None:
            if message["type"] == "http.response.start":
                status_holder["status"] = message["status"]
                raw = [
                    (key, value)
                    for key, value in message.get("headers", [])
                    if key.lower() not in {b"x-request-id", b"traceparent"}
                ]
                raw.append((b"x-request-id", request_id.encode("ascii")))
                if traceparent is not None:
                    raw.append((b"traceparent", traceparent.encode("ascii")))
                message = {**message, "headers": raw}
            await send(message)

        try:
            await self.app(scope, receive, send_with_context)
        finally:
            if scope.get("path") in {"/api/v1/ai/route", "/api/v1/ai/vision/products"}:
                elapsed_ms = int((time.perf_counter() - started) * 1000)
                log.info(
                    "AI routing finished status=%s durationMs=%s requestId=%s",
                    status_holder["status"],
                    elapsed_ms,
                    current_request_id() or request_id,
                )
            reset_request_id(request_token)
            reset_traceparent(trace_token)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings: Settings = app.state.settings_override
    configure_logging(settings.log_level)
    http = httpx.AsyncClient(
        timeout=httpx.Timeout(settings.ai_request_timeout_seconds, connect=3.0),
        limits=httpx.Limits(max_connections=100, max_keepalive_connections=20),
        follow_redirects=False,
    )
    provider = build_provider(settings, http)
    app.state.settings = settings
    app.state.http = http
    app.state.intent_service = IntentService(settings, AiProviderService(provider, settings))
    app.state.vision_service = VisionService(provider)
    log.info("AI inference started provider=%s", settings.ai_provider)
    try:
        yield
    finally:
        await http.aclose()


def create_app(settings: Settings | None = None) -> FastAPI:
    app = FastAPI(title="E-Shop AI Inference", lifespan=lifespan)
    app.state.settings_override = settings if settings is not None else Settings()
    app.add_exception_handler(AiRouteError, ai_route_error)
    app.add_exception_handler(RequestValidationError, invalid_request)
    app.include_router(router)
    app.include_router(vision_router)
    app.add_middleware(RequestContextMiddleware)
    return app


app = create_app()

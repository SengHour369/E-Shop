"""HTTP API. This process selects a tool. It does not execute one."""

from __future__ import annotations

import logging

from fastapi import APIRouter, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.config import readiness
from app.core.errors import PUBLIC_MESSAGE, AiRouteError
from app.core.request_context import current_request_id
from app.schemas.ai_request import AiRouteRequest
from app.schemas.ai_response import AiRouteResponse
from app.schemas.health import HealthResponse
from app.services.intent_service import IntentService

log = logging.getLogger(__name__)
router = APIRouter()


@router.post("/api/v1/ai/route", response_model=AiRouteResponse)
async def route_intent(body: AiRouteRequest, request: Request) -> AiRouteResponse:
    service: IntentService = request.app.state.intent_service
    return await service.route(body)


@router.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(status="UP")


@router.get("/ready", response_model=HealthResponse)
async def ready(request: Request):
    if not readiness(request.app.state.settings):
        return JSONResponse(status_code=503, content={"status": "NOT_READY"})
    return HealthResponse(status="READY")


async def ai_route_error(_request: Request, exc: AiRouteError) -> JSONResponse:
    log.warning("AI routing failed code=%s status=%s", exc.code, exc.status)
    return JSONResponse(
        status_code=exc.status,
        content={
            "code": exc.code,
            "message": exc.message or PUBLIC_MESSAGE,
            "requestId": current_request_id() or "-",
        },
    )


async def invalid_request(_request: Request, _exc: RequestValidationError) -> JSONResponse:
    """Do not echo the submitted message. It is untrusted and may contain secrets."""
    log.info("AI routing request rejected")
    return JSONResponse(
        status_code=422,
        content={
            "code": "AI_INVALID_PARAMETERS",
            "message": "The routing request is invalid.",
            "requestId": current_request_id() or "-",
        },
    )

"""Optional product-photo fallback. One still image in, names out."""

from fastapi import APIRouter, Request

from app.schemas.vision import VisionRequest, VisionResponse
from app.services.vision_service import VisionService

router = APIRouter()


@router.post("/api/v1/ai/vision/products", response_model=VisionResponse)
async def identify_products(body: VisionRequest, request: Request) -> VisionResponse:
    service: VisionService = request.app.state.vision_service
    candidates = await service.identify(body.media_type, body.image_base64)
    return VisionResponse(candidates=candidates)


from app.schemas.vision import VisualComparisonRequest, VisualComparisonResponse
from fastapi import Header
import hmac
from app.core.errors import AiRouteError


@router.post("/api/v1/ai/vision/compare", response_model=VisualComparisonResponse)
async def compare_products(body: VisualComparisonRequest, request: Request,
                           service_key: str = Header(default="", alias="X-Vision-Service-Key")) -> VisualComparisonResponse:
    configured = request.app.state.settings.ai_vision_service_key
    if not configured or not hmac.compare_digest(configured, service_key):
        raise AiRouteError("AI_SERVICE_UNAUTHORIZED", "Visual comparison is unavailable.", 403)
    service: VisionService = request.app.state.vision_service
    return VisualComparisonResponse(matches=await service.compare(body.media_type, body.image_base64, body.candidates))

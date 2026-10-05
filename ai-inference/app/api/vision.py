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

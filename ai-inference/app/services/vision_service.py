"""Turns one still image into product-name suggestions. It does not search the catalog."""

from __future__ import annotations

import base64
import binascii

from app.core.errors import AI_INVALID_PARAMETERS, AiRouteError
from app.providers.base import AiProvider
from app.schemas.vision import VisionCandidate

_MEDIA_TYPES = {"image/jpeg", "image/png", "image/webp"}
_MAX_BYTES = 1_500_000
_INVALID = "The image is invalid."


class VisionService:
    def __init__(self, provider: AiProvider) -> None:
        self.provider = provider

    async def identify(self, media_type: str, image_base64: str) -> list[VisionCandidate]:
        _validate(media_type, image_base64)
        raw = await self.provider.identify_products(media_type=media_type, image_base64=image_base64)
        return public_candidates(raw)


def public_candidates(raw: dict) -> list[VisionCandidate]:
    """Copy name and confidence only, so a model cannot supply a product id."""
    rows = raw.get("candidates") if isinstance(raw, dict) else None
    if not isinstance(rows, list):
        return []
    candidates: list[VisionCandidate] = []
    for row in rows:
        if len(candidates) == 5 or not isinstance(row, dict):
            continue
        name = row.get("name")
        confidence = row.get("confidence")
        if not isinstance(name, str) or not isinstance(confidence, (int, float)):
            continue
        cleaned = " ".join(name.split())
        if not cleaned or len(cleaned) > 160:
            continue
        bounded = max(0.0, min(1.0, float(confidence)))
        candidates.append(VisionCandidate(name=cleaned, confidence=round(bounded, 2)))
    return candidates


def _validate(media_type: str, image_base64: str) -> None:
    if media_type not in _MEDIA_TYPES:
        raise AiRouteError(AI_INVALID_PARAMETERS, _INVALID, 422)
    try:
        decoded = base64.b64decode(image_base64, validate=True)
    except (binascii.Error, ValueError):
        raise AiRouteError(AI_INVALID_PARAMETERS, _INVALID, 422) from None
    if not decoded or len(decoded) > _MAX_BYTES:
        raise AiRouteError(AI_INVALID_PARAMETERS, _INVALID, 422)

"""Turns one still image into product-name suggestions. It does not search the catalog."""

from __future__ import annotations

import base64
import binascii
import math

from app.core.errors import AI_INVALID_PARAMETERS, AiRouteError
from app.providers.base import AiProvider
from app.schemas.vision import VisionCandidate

_MEDIA_TYPES = {"image/jpeg", "image/png", "image/webp"}
_MAX_BYTES = 4000 * 1024 * 1024
_INVALID = "The image is invalid."


class VisionService:
    def __init__(self, provider: AiProvider) -> None:
        self.provider = provider

    async def identify(self, media_type: str, image_base64: str) -> list[VisionCandidate]:
        _validate(media_type, image_base64)
        raw = await self.provider.identify_products(media_type=media_type, image_base64=image_base64)
        return public_candidates(raw)

    async def compare(self, media_type: str, image_base64: str, candidates: list) -> list:
        from app.schemas.vision import VisualComparisonMatch
        _validate(media_type, image_base64)
        if len(base64.b64decode(image_base64)) > 8 * 1024 * 1024:
            raise AiRouteError(AI_INVALID_PARAMETERS, _INVALID, 422)
        for candidate in candidates:
            _validate(candidate.media_type, candidate.image_base64)
        raw = await self.provider.compare_products(media_type=media_type, image_base64=image_base64,
            candidates=[candidate.model_dump(by_alias=True) for candidate in candidates])
        rows = raw.get("matches") if isinstance(raw, dict) else None
        if not isinstance(rows, list):
            raise AiRouteError("AI_INVALID_RESPONSE", "Invalid visual comparison result.", 502)
        matches = {}
        for row in rows[:5]:
            if not isinstance(row, dict): continue
            index, confidence = row.get("index"), row.get("confidence")
            if type(index) is not int or not 0 <= index < len(candidates): continue
            if isinstance(confidence, bool) or not isinstance(confidence, (int,float)) or not math.isfinite(confidence) or not 0 <= confidence <= 1: continue
            matches[index] = max(matches.get(index, 0), confidence)
        return [VisualComparisonMatch(index=index,confidence=confidence) for index,confidence in
                sorted(matches.items(),key=lambda item: (-item[1],item[0]))]


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
        if not isinstance(name, str) or isinstance(confidence, bool) or not isinstance(confidence, (int, float)) or not math.isfinite(confidence):
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

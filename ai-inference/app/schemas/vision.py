"""Image recognition shapes. Names only: catalog ids are not part of this contract."""

from pydantic import BaseModel, ConfigDict, Field


class VisionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    media_type: str = Field(alias="mediaType")
    image_base64: str = Field(alias="imageBase64", min_length=1, max_length=2_000_000)


class VisionCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str = Field(min_length=1, max_length=160)
    confidence: float = Field(ge=0, le=1)


class VisionResponse(BaseModel):
    candidates: list[VisionCandidate]

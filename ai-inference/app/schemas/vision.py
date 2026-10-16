"""Image recognition shapes. Names only: catalog ids are not part of this contract."""

from pydantic import BaseModel, ConfigDict, Field


class VisionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    media_type: str = Field(alias="mediaType")
    image_base64: str = Field(alias="imageBase64", min_length=1, max_length=5_592_405_336)


class VisionCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str = Field(min_length=1, max_length=160)
    confidence: float = Field(ge=0, le=1)


class VisionResponse(BaseModel):
    candidates: list[VisionCandidate]


class ComparisonImage(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")
    media_type: str = Field(alias="mediaType")
    image_base64: str = Field(alias="imageBase64", min_length=1, max_length=700_000)


class VisualComparisonRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")
    media_type: str = Field(alias="mediaType")
    image_base64: str = Field(alias="imageBase64", min_length=1, max_length=11_184_812)
    candidates: list[ComparisonImage] = Field(min_length=1, max_length=5)


class VisualComparisonMatch(BaseModel):
    model_config = ConfigDict(extra="forbid")
    index: int = Field(ge=0, le=4)
    confidence: float = Field(ge=0, le=1)


class VisualComparisonResponse(BaseModel):
    matches: list[VisualComparisonMatch]

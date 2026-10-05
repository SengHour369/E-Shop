from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.schemas.tool_definition import ToolDefinition


class AiRouteRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    message: str = Field(min_length=1, max_length=4000)
    tools: list[ToolDefinition] = Field(default_factory=list, max_length=40)

    @model_validator(mode="after")
    def unique_tool_names(self) -> "AiRouteRequest":
        names = [tool.name for tool in self.tools]
        if len(names) != len(set(names)):
            raise ValueError("duplicate tool name")
        return self

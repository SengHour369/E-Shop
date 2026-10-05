"""Safe failures. Provider details and secrets stay out of these messages."""

PUBLIC_MESSAGE = "AI routing is temporarily unavailable"

AI_PROVIDER_TIMEOUT = "AI_PROVIDER_TIMEOUT"
AI_PROVIDER_ERROR = "AI_PROVIDER_ERROR"
AI_INVALID_RESPONSE = "AI_INVALID_RESPONSE"
AI_UNKNOWN_INTENT = "AI_UNKNOWN_INTENT"
AI_TOOL_NOT_ALLOWED = "AI_TOOL_NOT_ALLOWED"
AI_INVALID_PARAMETERS = "AI_INVALID_PARAMETERS"


class AiRouteError(Exception):
    """A provider or request failure with a controlled code and HTTP status."""

    def __init__(self, code: str, message: str, status: int) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status = status


class DecisionRejected(Exception):
    """Model output that must not be executed. The API turns this into UNKNOWN."""

    def __init__(self, code: str) -> None:
        super().__init__(code)
        self.code = code

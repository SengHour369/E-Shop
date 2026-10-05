"""Logs for routing. The user message, tokens, and provider secrets are not logged."""

from __future__ import annotations

import logging
import re

from app.core.request_context import current_request_id, current_trace_id

_BEARER = re.compile(r"Bearer\s+[A-Za-z0-9._\-]+", re.IGNORECASE)
_OPENAI_KEY = re.compile(r"sk-[A-Za-z0-9_\-]{8,}")


def redact(text: str) -> str:
    text = _BEARER.sub("Bearer [redacted]", text)
    return _OPENAI_KEY.sub("sk-[redacted]", text)


class ContextFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.requestId = current_request_id() or "-"
        record.traceId = current_trace_id() or "-"
        return True


class RedactSecretsFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        if not getattr(record, "_redacted", False):
            record.msg = redact(record.getMessage())
            record.args = ()
            record._redacted = True
        return True


def configure_logging(level: str) -> None:
    root = logging.getLogger()
    root.setLevel(level)
    if not any(getattr(handler, "_ai_inference", False) for handler in root.handlers):
        handler = logging.StreamHandler()
        handler._ai_inference = True
        handler.setFormatter(logging.Formatter(
            "%(asctime)s %(levelname)s service=ai-inference requestId=%(requestId)s "
            "traceId=%(traceId)s %(name)s %(message)s"
        ))
        handler.addFilter(ContextFilter())
        handler.addFilter(RedactSecretsFilter())
        root.addHandler(handler)
    logging.getLogger("app").setLevel(level)
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)

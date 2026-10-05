"""Request id and W3C traceparent for the current call.

The values are taken from Spring Boot. This service does not start its own trace.
"""

from __future__ import annotations

import re
import uuid
from contextvars import ContextVar

REQUEST_ID_HEADER = "X-Request-ID"
TRACEPARENT_HEADER = "traceparent"

_SAFE_REQUEST_ID = re.compile(r"^[A-Za-z0-9._:-]{1,128}$")
_TRACEPARENT = re.compile(
    r"00-(?!0{32})[a-f0-9]{32}-(?!0{16})[a-f0-9]{16}-[a-f0-9]{2}"
)

_request_id: ContextVar[str] = ContextVar("request_id", default="")
_traceparent: ContextVar[str | None] = ContextVar("traceparent", default=None)


def resolve_request_id(values: list[str]) -> str:
    """Use the single safe inbound id. Otherwise generate one."""
    if len(values) == 1 and _SAFE_REQUEST_ID.fullmatch(values[0]):
        return values[0]
    return "req_" + str(uuid.uuid4())


def valid_traceparent(value: str | None) -> bool:
    return value is not None and _TRACEPARENT.fullmatch(value) is not None


def current_request_id() -> str:
    return _request_id.get()


def current_traceparent() -> str | None:
    return _traceparent.get()


def current_trace_id() -> str:
    parent = _traceparent.get()
    if parent is not None and valid_traceparent(parent):
        return parent[3:35]
    return ""


def set_request_id(value: str):
    return _request_id.set(value)


def reset_request_id(token) -> None:
    _request_id.reset(token)


def set_traceparent(value: str | None):
    return _traceparent.set(value)


def reset_traceparent(token) -> None:
    _traceparent.reset(token)

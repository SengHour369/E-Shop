"""Parameter types and write tools. Authorization stays in Spring Boot."""

from __future__ import annotations

import math
import re
from typing import Any

ALLOWED_PARAMETER_TYPES = frozenset({"boolean", "integer", "number", "string"})

# Spring Boot makes the final risk decision. These names ask for confirmation.
WRITE_TOOLS = frozenset({
    "ORDER_CANCEL",
    "PROMOTION_CREATE",
    "PROMOTION_DISABLE",
    "PAYMENT_REFUND",
})

_CONTROL = re.compile(r"[\x00-\x1f\x7f]")
_LONG_MIN = -9223372036854775808
_LONG_MAX = 9223372036854775807


def requires_confirmation(tool_name: str) -> bool:
    return tool_name in WRITE_TOOLS


def value_matches(type_name: str, value: Any) -> bool:
    if type_name == "boolean":
        return isinstance(value, bool)
    if type_name == "integer":
        return isinstance(value, int) and not isinstance(value, bool) and _LONG_MIN <= value <= _LONG_MAX
    if type_name == "number":
        if isinstance(value, bool) or not isinstance(value, (int, float)):
            return False
        return math.isfinite(float(value))
    if type_name == "string":
        return isinstance(value, str) and 0 < len(value) <= 4000 and _CONTROL.search(value) is None
    return False

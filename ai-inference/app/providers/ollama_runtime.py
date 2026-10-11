"""Bound CPU inference queues per shared HTTP client; never cache private replies."""
import asyncio
from contextlib import asynccontextmanager
from weakref import WeakKeyDictionary
from app.core.errors import AiRouteError

_slots = WeakKeyDictionary()

@asynccontextmanager
async def ollama_slot(http):
    slot = _slots.setdefault(http, asyncio.Semaphore(1))
    try:
        await asyncio.wait_for(slot.acquire(), timeout=1.0)
    except TimeoutError:
        raise AiRouteError("AI_PROVIDER_BUSY", "The assistant is busy. Please try again shortly.", 429) from None
    try:
        yield
    finally:
        slot.release()

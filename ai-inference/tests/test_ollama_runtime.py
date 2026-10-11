import asyncio
import httpx
import pytest
from app.core.errors import AiRouteError
from app.providers.ollama_runtime import ollama_slot

@pytest.mark.asyncio
async def test_busy_model_fails_fast_and_releases_slot_after_failure():
    async with httpx.AsyncClient() as http:
        with pytest.raises(RuntimeError):
            async with ollama_slot(http):
                with pytest.raises(AiRouteError) as error:
                    async with ollama_slot(http):
                        pytest.fail("second request entered an occupied slot")
                assert error.value.status == 429
                assert error.value.code == "AI_PROVIDER_BUSY"
                raise RuntimeError("provider failed")
        async with ollama_slot(http):
            pass

@pytest.mark.asyncio
async def test_cancellation_releases_the_model_slot():
    async with httpx.AsyncClient() as http:
        entered = asyncio.Event()
        async def pending():
            async with ollama_slot(http):
                entered.set()
                await asyncio.Event().wait()
        task = asyncio.create_task(pending())
        await entered.wait()
        task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await task
        async with ollama_slot(http):
            pass

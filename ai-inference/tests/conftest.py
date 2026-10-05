import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from app.services.intent_service import IntentService
from app.services.provider_service import AiProviderService
from app.services.vision_service import VisionService
from tests.support import FakeProvider, make_settings


@pytest.fixture
def fake() -> FakeProvider:
    return FakeProvider()


@pytest.fixture
def settings():
    return make_settings()


@pytest.fixture
def service(fake: FakeProvider, settings):
    return IntentService(settings, AiProviderService(fake, settings))


@pytest.fixture
def client(fake: FakeProvider, settings):
    app = create_app(settings)
    with TestClient(app) as test_client:
        app.state.intent_service = IntentService(settings, AiProviderService(fake, settings))
        app.state.vision_service = VisionService(fake)
        yield test_client

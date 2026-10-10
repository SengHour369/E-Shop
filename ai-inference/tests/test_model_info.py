from fastapi.testclient import TestClient
from app.main import create_app
from tests.support import make_settings

def test_model_info_reports_configuration_without_credentials_or_provider_calls():
    app = create_app(make_settings(ai_provider='OLLAMA', ollama_model='local-test'))
    with TestClient(app) as client:
        response = client.get('/api/v1/ai/info')
        assert response.status_code == 200
        assert response.json() == {'provider':'OLLAMA','model':'local-test','configured':True,'historyMinutes':30}
        assert 'key' not in response.text and 'base_url' not in response.text

import pytest
from fastapi.testclient import TestClient

from app import config
from app.db import get_db
from app.main import app

KEY = {'X-Internal-Api-Key': 'test-internal-key'}


class FakeConnection:
    def __init__(self, error: Exception | None = None):
        self.error = error
        self.executed: list[str] = []

    def execute(self, sql: str) -> None:
        if self.error:
            raise self.error
        self.executed.append(sql)


@pytest.fixture
def conn():
    return FakeConnection()


@pytest.fixture
def client(monkeypatch, conn):
    monkeypatch.setattr(config, 'INTERNAL_API_KEY', 'test-internal-key')
    app.dependency_overrides[get_db] = lambda: conn
    yield TestClient(app, raise_server_exceptions=False)
    app.dependency_overrides.clear()


def assert_error(res, status: int, code: str) -> dict:
    body = res.json()
    assert res.status_code == status
    assert sorted(body) == ['code', 'data', 'message', 'success']
    assert (body['success'], body['data'], body['code']) == (False, None, code)
    assert body['message']
    return body


def test_health_returns_up_with_valid_key(client, conn):
    res = client.get('/internal/v1/health', headers=KEY)
    assert res.status_code == 200
    assert res.json() == {'success': True, 'data': {'status': 'UP'}}
    assert conn.executed == ['SELECT 1']


def test_missing_key_returns_401_without_touching_db(client, conn):
    body = assert_error(client.get('/internal/v1/health'), 401, 'INTERNAL-001')
    assert body['message'] == '内部APIキーが不正です。'
    assert conn.executed == []


def test_wrong_key_returns_401(client):
    assert_error(client.get('/internal/v1/health', headers={'X-Internal-Api-Key': 'wrong'}), 401, 'INTERNAL-001')


def test_non_ascii_key_returns_401(client):
    res = client.get('/internal/v1/health', headers={'X-Internal-Api-Key': 'ü'.encode()})
    assert_error(res, 401, 'INTERNAL-001')


def test_unset_key_rejects_empty_header(client, monkeypatch):
    monkeypatch.setattr(config, 'INTERNAL_API_KEY', '')
    assert_error(client.get('/internal/v1/health', headers={'X-Internal-Api-Key': ''}), 401, 'INTERNAL-001')


def test_unknown_url_returns_404(client):
    body = assert_error(client.get('/internal/v1/unknown', headers=KEY), 404, 'COMMON-001')
    assert body['message'] == '存在しないURLです。'


def test_unsupported_method_returns_405(client):
    assert_error(client.post('/internal/v1/health', headers=KEY), 405, 'COMMON-002')


def test_api_docs_are_not_exposed(client):
    for path in ('/docs', '/redoc', '/openapi.json'):
        assert_error(client.get(path), 404, 'COMMON-001')


def test_unexpected_error_returns_500_without_details(client, conn):
    conn.error = RuntimeError('internal secret detail')
    res = client.get('/internal/v1/health', headers=KEY)
    body = assert_error(res, 500, 'SERVER-001')
    assert body['message'] == 'サーバーでエラーが発生しました。時間をおいて再度お試しください。'
    assert 'internal secret detail' not in res.text

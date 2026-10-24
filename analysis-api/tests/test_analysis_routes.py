import datetime as dt
from decimal import Decimal

import pytest
from fastapi.testclient import TestClient

from app import config, repository
from app.db import get_db
from app.main import app
from app.repository import HealthRecordRow, VaccinationRow
from app.routers import get_today

KEY = {'X-Internal-Api-Key': 'test-internal-key'}
TODAY = dt.date(2026, 10, 25)
BASE = '/internal/v1/analysis/pets/7'


class Calls:
    def __init__(self):
        self.health_records = []
        self.vaccinations = []


@pytest.fixture
def calls(monkeypatch):
    calls = Calls()

    def find_health_records(conn, pet_id, start, end):
        calls.health_records.append((pet_id, start, end))
        return [HealthRecordRow(1, dt.date(2026, 10, 1), Decimal('4.00'), None),
                HealthRecordRow(2, dt.date(2026, 10, 20), Decimal('4.20'), '咳')]

    def find_vaccinations(conn, pet_id):
        calls.vaccinations.append(pet_id)
        return [VaccinationRow(id=3, vaccine_name='狂犬病', scheduled_date=dt.date(2025, 11, 1),
                               completed_date=dt.date(2025, 11, 1), status='COMPLETED')]

    monkeypatch.setattr(repository, 'find_health_records', find_health_records)
    monkeypatch.setattr(repository, 'find_vaccinations', find_vaccinations)
    return calls


@pytest.fixture
def client(monkeypatch, calls):
    monkeypatch.setattr(config, 'INTERNAL_API_KEY', 'test-internal-key')
    app.dependency_overrides[get_db] = lambda: object()
    app.dependency_overrides[get_today] = lambda: TODAY
    yield TestClient(app, raise_server_exceptions=False)
    app.dependency_overrides.clear()


def assert_error(res, status, code):
    assert res.status_code == status
    assert res.json() == {'success': False, 'data': None, 'code': code, 'message': res.json()['message']}


def test_weight_returns_camel_case_envelope_and_passes_period(client, calls):
    res = client.get(f'{BASE}/weight?from=2026-10-01&to=2026-10-25', headers=KEY)
    assert res.status_code == 200
    assert res.json() == {'success': True, 'data': {
        'points': [{'date': '2026-10-01', 'weight': 4.0}, {'date': '2026-10-20', 'weight': 4.2}],
        'summary': {'count': 2, 'first': 4.0, 'latest': 4.2, 'min': 4.0, 'max': 4.2, 'change': 0.2, 'changeRate': 5.0},
    }}
    assert calls.health_records == [(7, dt.date(2026, 10, 1), dt.date(2026, 10, 25))]


def test_weight_without_period(client, calls):
    assert client.get(f'{BASE}/weight', headers=KEY).status_code == 200
    assert calls.health_records == [(7, None, None)]


def test_weight_rejects_reversed_period_without_query(client, calls):
    assert_error(client.get(f'{BASE}/weight?from=2026-10-25&to=2026-10-01', headers=KEY), 400, 'VALIDATION-001')
    assert calls.health_records == []


@pytest.mark.parametrize('query', ['from=2026-13-01', 'to=abc', 'from=2026/10/01'])
def test_weight_rejects_bad_date_as_400_not_422(client, query):
    assert_error(client.get(f'{BASE}/weight?{query}', headers=KEY), 400, 'VALIDATION-001')


def test_non_numeric_pet_id_is_400(client):
    assert_error(client.get('/internal/v1/analysis/pets/abc/weight', headers=KEY), 400, 'VALIDATION-001')


def test_health_score_reads_the_30_day_window(client, calls):
    res = client.get(f'{BASE}/health-score', headers=KEY)
    assert res.status_code == 200
    data = res.json()['data']
    assert (data['score'], data['grade'], data['period']) == (75, 'CAUTION', {'from': '2026-09-26', 'to': '2026-10-25'})
    assert data['factors'] == [
        {'type': 'WEIGHT_CHANGE', 'evaluated': True, 'penalty': 15, 'value': 5.0},
        {'type': 'SYMPTOM_FREQUENCY', 'evaluated': True, 'penalty': 10, 'value': 1},
    ]
    assert isinstance(data['factors'][1]['value'], int) and isinstance(data['factors'][0]['value'], float)
    assert calls.health_records == [(7, dt.date(2026, 9, 26), TODAY)]


def test_vaccination_next(client, calls):
    res = client.get(f'{BASE}/vaccination/next', headers=KEY)
    assert res.json() == {'success': True, 'data': {'items': [{
        'vaccineName': '狂犬病', 'lastCompletedDate': '2025-11-01', 'nextDate': '2026-11-01', 'source': 'RECOMMENDED',
        'vaccinationId': None, 'overdue': False, 'daysUntil': 7,
    }]}}
    assert calls.vaccinations == [7]


@pytest.mark.parametrize('path', ['weight', 'health-score', 'vaccination/next'])
def test_analysis_routes_require_key(client, calls, path):
    assert_error(client.get(f'{BASE}/{path}'), 401, 'INTERNAL-001')
    assert (calls.health_records, calls.vaccinations) == ([], [])


def test_symptoms_summary_is_not_implemented(client):
    # 練習用ギャップ（docs/07 §7-3）
    assert_error(client.get(f'{BASE}/symptoms/summary', headers=KEY), 404, 'COMMON-001')

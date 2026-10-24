import datetime as dt

import psycopg
from fastapi import APIRouter, Depends, Query

from . import repository, services
from .db import get_db
from .errors import VALIDATION_ERROR, ApiError
from .schemas import ApiResponse, HealthScoreData, NextVaccinationData, WeightData
from .security import require_internal_api_key

# Spring API からだけ呼ばれる内部API（docs/07 §7-5）。ペットの所有者チェックは Spring が行う
internal = APIRouter(prefix='/internal/v1', dependencies=[Depends(require_internal_api_key)])


def get_today() -> dt.date:
    return services.today_jst()


@internal.get('/health')
def health(conn: psycopg.Connection = Depends(get_db)) -> dict:
    """DB接続を含む稼働確認。"""
    conn.execute('SELECT 1')
    return {'success': True, 'data': {'status': 'UP'}}


@internal.get('/analysis/pets/{pet_id}/weight', response_model=ApiResponse[WeightData])
def weight(pet_id: int,
           start: dt.date | None = Query(None, alias='from'),
           end: dt.date | None = Query(None, alias='to'),
           conn: psycopg.Connection = Depends(get_db)):
    if start is not None and end is not None and start > end:
        raise ApiError(*VALIDATION_ERROR)
    return ApiResponse(data=services.analyze_weight(repository.find_health_records(conn, pet_id, start, end)))


@internal.get('/analysis/pets/{pet_id}/health-score', response_model=ApiResponse[HealthScoreData])
def health_score(pet_id: int, today: dt.date = Depends(get_today), conn: psycopg.Connection = Depends(get_db)):
    start, end = services.score_period(today)
    return ApiResponse(data=services.health_score(repository.find_health_records(conn, pet_id, start, end), today))


@internal.get('/analysis/pets/{pet_id}/vaccination/next', response_model=ApiResponse[NextVaccinationData])
def vaccination_next(pet_id: int, today: dt.date = Depends(get_today), conn: psycopg.Connection = Depends(get_db)):
    return ApiResponse(data=services.next_vaccinations(repository.find_vaccinations(conn, pet_id), today))

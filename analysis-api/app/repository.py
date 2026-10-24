import datetime as dt
from decimal import Decimal
from typing import NamedTuple

import psycopg
from psycopg.rows import class_row


class HealthRecordRow(NamedTuple):
    id: int
    recorded_date: dt.date
    weight: Decimal | None
    symptom: str | None


class VaccinationRow(NamedTuple):
    id: int
    vaccine_name: str
    scheduled_date: dt.date
    completed_date: dt.date | None
    status: str


def find_health_records(conn: psycopg.Connection, pet_id: int,
                        start: dt.date | None, end: dt.date | None) -> list[HealthRecordRow]:
    """論理削除されていない健康記録を記録日・登録順に返す。期間は両端を含み、省略した側は制限しない。"""
    sql = 'SELECT id, recorded_date, weight, symptom FROM health_records WHERE pet_id = %s AND deleted_at IS NULL'
    params: list = [pet_id]
    if start is not None:
        sql += ' AND recorded_date >= %s'
        params.append(start)
    if end is not None:
        sql += ' AND recorded_date <= %s'
        params.append(end)
    with conn.cursor(row_factory=class_row(HealthRecordRow)) as cur:
        return cur.execute(sql + ' ORDER BY recorded_date, id', params).fetchall()


def find_vaccinations(conn: psycopg.Connection, pet_id: int) -> list[VaccinationRow]:
    """論理削除されていないワクチン接種予定を返す。"""
    with conn.cursor(row_factory=class_row(VaccinationRow)) as cur:
        return cur.execute(
            'SELECT id, vaccine_name, scheduled_date, completed_date, status FROM vaccinations'
            ' WHERE pet_id = %s AND deleted_at IS NULL ORDER BY id',
            [pet_id],
        ).fetchall()

"""分析ロジック（docs/07 §7）。DBに依存しない関数にして、記録の一覧から結果を作る。"""
import datetime as dt
from collections import defaultdict
from collections.abc import Iterable
from decimal import ROUND_HALF_UP, Decimal
from zoneinfo import ZoneInfo

from .repository import HealthRecordRow, VaccinationRow
from .schemas import (HealthScoreData, HealthScoreFactor, NextVaccination, NextVaccinationData, Period, WeightData,
                      WeightPoint, WeightSummary)

JST = ZoneInfo('Asia/Tokyo')
SCORE_WINDOW_DAYS = 30


def today_jst() -> dt.date:
    return dt.datetime.now(JST).date()


def daily_weights(records: Iterable[HealthRecordRow]) -> list[tuple[dt.date, Decimal]]:
    """体重が入力された記録を日別にする。同じ日に複数あれば最後に登録した記録（idが最大）の体重を使う。"""
    latest: dict[dt.date, tuple[int, Decimal]] = {}
    for r in records:
        if r.weight is not None and (r.recorded_date not in latest or r.id > latest[r.recorded_date][0]):
            latest[r.recorded_date] = (r.id, r.weight)
    return sorted((day, weight) for day, (_, weight) in latest.items())


def change_rate(first: Decimal, latest: Decimal) -> Decimal | None:
    """変化率（%、小数1桁に四捨五入）。最初の値が0なら計算できない。"""
    if first == 0:
        return None
    return ((latest - first) / first * 100).quantize(Decimal('0.1'), ROUND_HALF_UP)


def analyze_weight(records: Iterable[HealthRecordRow]) -> WeightData:
    points = daily_weights(records)
    if not points:
        return WeightData(points=[], summary=None)
    weights = [weight for _, weight in points]
    first, latest = weights[0], weights[-1]
    return WeightData(
        points=[WeightPoint(date=day, weight=weight) for day, weight in points],
        summary=WeightSummary(count=len(points), first=first, latest=latest, min=min(weights), max=max(weights),
                              change=latest - first, change_rate=change_rate(first, latest)),
    )


def score_period(today: dt.date) -> tuple[dt.date, dt.date]:
    """健康スコアの評価期間（今日を含む直近30日）。"""
    return today - dt.timedelta(days=SCORE_WINDOW_DAYS - 1), today


def _weight_factor(points: list[tuple[dt.date, Decimal]]) -> HealthScoreFactor:
    rate = change_rate(points[0][1], points[-1][1]) if len(points) >= 2 else None
    if rate is None:
        return HealthScoreFactor(type='WEIGHT_CHANGE', evaluated=False, penalty=0, value=None)
    rate = abs(rate)
    # 減点の境界は表示する値（小数1桁）で判定する
    penalty = 30 if rate >= 10 else 15 if rate >= 5 else 0
    return HealthScoreFactor(type='WEIGHT_CHANGE', evaluated=True, penalty=penalty, value=float(rate))


def health_score(records: Iterable[HealthRecordRow], today: dt.date) -> HealthScoreData:
    start, end = score_period(today)
    in_period = [r for r in records if start <= r.recorded_date <= end]
    period = Period(from_=start, to=end)
    if not in_period:
        return HealthScoreData(score=None, grade='NO_DATA', period=period, factors=[
            HealthScoreFactor(type='WEIGHT_CHANGE', evaluated=False, penalty=0, value=None),
            HealthScoreFactor(type='SYMPTOM_FREQUENCY', evaluated=False, penalty=0, value=None),
        ])

    weight = _weight_factor(daily_weights(in_period))
    symptom_days = len({r.recorded_date for r in in_period if r.symptom and r.symptom.strip()})
    symptom = HealthScoreFactor(type='SYMPTOM_FREQUENCY', evaluated=True, penalty=min(symptom_days * 10, 40),
                                value=symptom_days)
    score = 100 - weight.penalty - symptom.penalty
    grade = 'GOOD' if score >= 80 else 'CAUTION' if score >= 50 else 'WARNING'
    return HealthScoreData(score=score, grade=grade, period=period, factors=[weight, symptom])


def one_year_later(day: dt.date) -> dt.date:
    try:
        return day.replace(year=day.year + 1)
    except ValueError:  # 2/29 の翌年
        return day.replace(year=day.year + 1, day=28)


def next_vaccinations(rows: Iterable[VaccinationRow], today: dt.date) -> NextVaccinationData:
    """ワクチン名ごとの次の接種日。予定（SCHEDULED）があれば最も早い予定日、無ければ最後の接種日の1年後。"""
    by_name: dict[str, list[VaccinationRow]] = defaultdict(list)
    for row in rows:
        by_name[row.vaccine_name.strip()].append(row)

    items = []
    for name, group in by_name.items():
        last_completed = max((v.completed_date for v in group if v.status == 'COMPLETED' and v.completed_date),
                             default=None)
        scheduled = min((v for v in group if v.status == 'SCHEDULED'), key=lambda v: (v.scheduled_date, v.id),
                        default=None)
        if scheduled is not None:
            next_date, source, vaccination_id = scheduled.scheduled_date, 'SCHEDULED', scheduled.id
        elif last_completed is not None:
            next_date, source, vaccination_id = one_year_later(last_completed), 'RECOMMENDED', None
        else:
            continue
        items.append(NextVaccination(vaccine_name=name, last_completed_date=last_completed, next_date=next_date,
                                     source=source, vaccination_id=vaccination_id, overdue=next_date < today,
                                     days_until=(next_date - today).days))
    items.sort(key=lambda item: (item.next_date, item.vaccine_name))
    return NextVaccinationData(items=items)

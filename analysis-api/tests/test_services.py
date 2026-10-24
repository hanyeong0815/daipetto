import datetime as dt
from decimal import Decimal

from app.repository import HealthRecordRow, VaccinationRow
from app.services import analyze_weight, daily_weights, health_score, next_vaccinations, one_year_later

TODAY = dt.date(2026, 10, 25)


def rec(id_, day, weight=None, symptom=None):
    return HealthRecordRow(id=id_, recorded_date=day, weight=None if weight is None else Decimal(weight), symptom=symptom)


def vac(id_, name, scheduled, status, completed=None):
    return VaccinationRow(id=id_, vaccine_name=name, scheduled_date=scheduled, completed_date=completed, status=status)


def days_ago(n):
    return TODAY - dt.timedelta(days=n)


# ---- 7-1 体重推移

def test_daily_weights_use_latest_registered_record_with_weight():
    day = dt.date(2026, 5, 1)
    records = [rec(5, day, '4.50'), rec(3, day, '4.30'), rec(9, day, None, '咳'), rec(1, dt.date(2026, 4, 1), '4.00')]
    assert daily_weights(records) == [(dt.date(2026, 4, 1), Decimal('4.00')), (day, Decimal('4.50'))]


def test_analyze_weight_summary():
    data = analyze_weight([rec(1, dt.date(2026, 5, 1), '4.30'), rec(2, dt.date(2026, 5, 10), '4.50'),
                           rec(3, dt.date(2026, 5, 5), '4.20')]).model_dump(by_alias=True)
    assert [p['weight'] for p in data['points']] == [4.3, 4.2, 4.5]
    assert data['summary'] == {'count': 3, 'first': 4.3, 'latest': 4.5, 'min': 4.2, 'max': 4.5,
                               'change': 0.2, 'changeRate': 4.7}


def test_analyze_weight_without_records():
    assert analyze_weight([rec(1, TODAY, None, '咳')]).model_dump() == {'points': [], 'summary': None}


def test_analyze_weight_change_rate_is_null_when_first_weight_is_zero():
    data = analyze_weight([rec(1, days_ago(2), '0'), rec(2, TODAY, '1.00')])
    assert data.summary.change_rate is None


def test_analyze_weight_rounds_half_up_including_negative():
    # 4.00 → 3.998: -0.05% → -0.1（四捨五入、0から遠い方）
    data = analyze_weight([rec(1, days_ago(1), '4.00'), rec(2, TODAY, '3.998')])
    assert data.summary.change_rate == -0.1


# ---- 7-2 健康スコア

def factors(data):
    return {f.type: (f.evaluated, f.penalty, f.value) for f in data.factors}


def test_health_score_no_data():
    data = health_score([rec(1, days_ago(30), '4.00', '咳')], TODAY)
    assert (data.score, data.grade) == (None, 'NO_DATA')
    assert (data.period.from_, data.period.to) == (days_ago(29), TODAY)
    assert factors(data) == {'WEIGHT_CHANGE': (False, 0, None), 'SYMPTOM_FREQUENCY': (False, 0, None)}


def test_health_score_without_symptoms_and_single_weight_is_good():
    data = health_score([rec(1, days_ago(29), '4.00')], TODAY)
    assert (data.score, data.grade) == (100, 'GOOD')
    assert factors(data) == {'WEIGHT_CHANGE': (False, 0, None), 'SYMPTOM_FREQUENCY': (True, 0, 0)}


def test_health_score_weight_change_thresholds():
    def penalty(latest):
        return factors(health_score([rec(1, days_ago(10), '4.00'), rec(2, TODAY, latest)], TODAY))['WEIGHT_CHANGE']

    assert penalty('4.19') == (True, 0, 4.8)
    assert penalty('4.20') == (True, 15, 5.0)
    assert penalty('3.75') == (True, 15, 6.3)
    assert penalty('4.40') == (True, 30, 10.0)
    assert penalty('3.50') == (True, 30, 12.5)


def test_health_score_rate_on_rounded_value():
    # 4.00 → 4.198 は 4.95% → 小数1桁で 5.0% として減点する
    data = health_score([rec(1, days_ago(10), '4.00'), rec(2, TODAY, '4.198')], TODAY)
    assert factors(data)['WEIGHT_CHANGE'] == (True, 15, 5.0)


def test_health_score_counts_symptom_days_once_and_caps_penalty():
    records = [rec(1, days_ago(1), symptom='咳'), rec(2, days_ago(1), symptom='嘔吐'), rec(3, days_ago(2), symptom='  '),
               rec(4, days_ago(3), symptom='咳'), rec(5, days_ago(31), symptom='咳')]
    assert factors(health_score(records, TODAY))['SYMPTOM_FREQUENCY'] == (True, 20, 2)
    many = [rec(i, days_ago(i), symptom='咳') for i in range(10)]
    assert factors(health_score(many, TODAY))['SYMPTOM_FREQUENCY'] == (True, 40, 10)


def test_health_score_grades():
    def score(weight_latest, symptom_days):
        records = [rec(100, days_ago(20), '4.00'), rec(101, TODAY, weight_latest)]
        records += [rec(i, days_ago(i + 1), symptom='咳') for i in range(symptom_days)]
        data = health_score(records, TODAY)
        return data.score, data.grade

    assert score('4.00', 2) == (80, 'GOOD')
    assert score('4.20', 1) == (75, 'CAUTION')
    assert score('4.40', 2) == (50, 'CAUTION')
    assert score('4.20', 4) == (45, 'WARNING')
    assert score('4.40', 5) == (30, 'WARNING')


# ---- 7-4 次回接種推奨日

def test_next_vaccinations_rules_and_order():
    rows = [
        vac(1, '狂犬病', dt.date(2025, 11, 1), 'COMPLETED', dt.date(2025, 11, 1)),
        vac(2, ' 混合ワクチン ', dt.date(2026, 9, 1), 'COMPLETED', dt.date(2026, 9, 3)),
        vac(3, '混合ワクチン', dt.date(2026, 12, 1), 'SCHEDULED'),
        vac(4, '混合ワクチン', dt.date(2026, 11, 20), 'SCHEDULED'),
        vac(5, 'フィラリア', dt.date(2026, 6, 1), 'CANCELLED'),
        vac(6, 'ノミ予防', dt.date(2025, 10, 1), 'COMPLETED', dt.date(2025, 10, 1)),
    ]
    items = [i.model_dump(by_alias=True) for i in next_vaccinations(rows, TODAY).items]
    assert items == [
        {'vaccineName': 'ノミ予防', 'lastCompletedDate': dt.date(2025, 10, 1), 'nextDate': dt.date(2026, 10, 1),
         'source': 'RECOMMENDED', 'vaccinationId': None, 'overdue': True, 'daysUntil': -24},
        {'vaccineName': '狂犬病', 'lastCompletedDate': dt.date(2025, 11, 1), 'nextDate': dt.date(2026, 11, 1),
         'source': 'RECOMMENDED', 'vaccinationId': None, 'overdue': False, 'daysUntil': 7},
        {'vaccineName': '混合ワクチン', 'lastCompletedDate': dt.date(2026, 9, 3), 'nextDate': dt.date(2026, 11, 20),
         'source': 'SCHEDULED', 'vaccinationId': 4, 'overdue': False, 'daysUntil': 26},
    ]


def test_next_vaccinations_same_day_schedule_uses_smaller_id_and_latest_completion():
    rows = [
        vac(9, 'A', dt.date(2026, 11, 1), 'SCHEDULED'),
        vac(7, 'A', dt.date(2026, 11, 1), 'SCHEDULED'),
        vac(1, 'A', dt.date(2024, 5, 1), 'COMPLETED', dt.date(2024, 5, 1)),
        vac(2, 'A', dt.date(2025, 5, 1), 'COMPLETED', dt.date(2025, 5, 3)),
        vac(3, 'A', dt.date(2025, 1, 1), 'COMPLETED', dt.date(2025, 1, 1)),
    ]
    item = next_vaccinations(rows, TODAY).items[0]
    assert (item.vaccination_id, item.last_completed_date) == (7, dt.date(2025, 5, 3))
    completed_only = next_vaccinations([r for r in rows if r.status == 'COMPLETED'], TODAY).items[0]
    assert (completed_only.next_date, completed_only.source) == (dt.date(2026, 5, 3), 'RECOMMENDED')


def test_next_vaccinations_due_today_is_not_overdue():
    item = next_vaccinations([vac(1, 'A', TODAY, 'SCHEDULED')], TODAY).items[0]
    assert (item.overdue, item.days_until) == (False, 0)


def test_next_vaccinations_empty():
    assert next_vaccinations([vac(1, 'A', TODAY, 'CANCELLED')], TODAY).items == []


def test_one_year_later_handles_leap_day():
    assert one_year_later(dt.date(2028, 2, 29)) == dt.date(2029, 2, 28)
    assert one_year_later(dt.date(2026, 3, 1)) == dt.date(2027, 3, 1)

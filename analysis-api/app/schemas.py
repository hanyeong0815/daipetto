import datetime as dt
from typing import Generic, Literal, TypeVar

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

T = TypeVar('T')


class Schema(BaseModel):
    # JSON は Spring と同じ camelCase
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class ApiResponse(Schema, Generic[T]):
    success: bool = True
    data: T


class WeightPoint(Schema):
    date: dt.date
    weight: float


class WeightSummary(Schema):
    count: int
    first: float
    latest: float
    min: float
    max: float
    change: float
    change_rate: float | None


class WeightData(Schema):
    points: list[WeightPoint]
    summary: WeightSummary | None


class Period(Schema):
    from_: dt.date = Field(alias='from')
    to: dt.date


class HealthScoreFactor(Schema):
    type: Literal['WEIGHT_CHANGE', 'SYMPTOM_FREQUENCY']
    evaluated: bool
    penalty: int
    # WEIGHT_CHANGE は変化率（%、float）、SYMPTOM_FREQUENCY は日数（int）
    value: int | float | None


class HealthScoreData(Schema):
    score: int | None
    grade: Literal['GOOD', 'CAUTION', 'WARNING', 'NO_DATA']
    period: Period
    factors: list[HealthScoreFactor]


class NextVaccination(Schema):
    vaccine_name: str
    last_completed_date: dt.date | None
    next_date: dt.date
    source: Literal['SCHEDULED', 'RECOMMENDED']
    vaccination_id: int | None
    overdue: bool
    days_until: int


class NextVaccinationData(Schema):
    items: list[NextVaccination]

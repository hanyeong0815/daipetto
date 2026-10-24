package koh.portfolio.springapi.domain.analysis.port;

import koh.portfolio.springapi.domain.analysis.model.HealthScore;
import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;

import java.time.LocalDate;

// 分析API（FastAPI、07 §7-5）への問い合わせ。所有者チェックは呼び出し側で済ませておく
public interface AnalysisClient {
    WeightAnalysis getWeight(Long petId, LocalDate from, LocalDate to);

    HealthScore getHealthScore(Long petId);

    VaccinationForecast getVaccinationForecast(Long petId);
}

package koh.portfolio.springapi.application.analysis.usecase;

import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;

public interface GetVaccinationForecastUseCase {
    VaccinationForecast execute(Long userId, Long petId);
}

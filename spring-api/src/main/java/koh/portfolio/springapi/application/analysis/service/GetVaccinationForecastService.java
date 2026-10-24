package koh.portfolio.springapi.application.analysis.service;

import koh.portfolio.springapi.application.analysis.usecase.GetVaccinationForecastUseCase;
import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;
import koh.portfolio.springapi.domain.analysis.port.AnalysisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetVaccinationForecastService implements GetVaccinationForecastUseCase {
    private final AnalysisPetChecker analysisPetChecker;
    private final AnalysisClient analysisClient;

    @Override
    public VaccinationForecast execute(Long userId, Long petId) {
        analysisPetChecker.checkOwner(userId, petId);

        return analysisClient.getVaccinationForecast(petId);
    }
}

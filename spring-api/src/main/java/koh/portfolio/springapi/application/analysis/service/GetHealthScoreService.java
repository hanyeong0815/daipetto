package koh.portfolio.springapi.application.analysis.service;

import koh.portfolio.springapi.application.analysis.usecase.GetHealthScoreUseCase;
import koh.portfolio.springapi.domain.analysis.model.HealthScore;
import koh.portfolio.springapi.domain.analysis.port.AnalysisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetHealthScoreService implements GetHealthScoreUseCase {
    private final AnalysisPetChecker analysisPetChecker;
    private final AnalysisClient analysisClient;

    @Override
    public HealthScore execute(Long userId, Long petId) {
        analysisPetChecker.checkOwner(userId, petId);

        return analysisClient.getHealthScore(petId);
    }
}

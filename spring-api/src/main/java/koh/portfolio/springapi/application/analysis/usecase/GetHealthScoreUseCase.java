package koh.portfolio.springapi.application.analysis.usecase;

import koh.portfolio.springapi.domain.analysis.model.HealthScore;

public interface GetHealthScoreUseCase {
    HealthScore execute(Long userId, Long petId);
}

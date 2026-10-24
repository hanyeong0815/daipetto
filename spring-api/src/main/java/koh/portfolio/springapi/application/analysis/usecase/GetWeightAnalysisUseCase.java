package koh.portfolio.springapi.application.analysis.usecase;

import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;

import java.time.LocalDate;

public interface GetWeightAnalysisUseCase {
    WeightAnalysis execute(Long userId, Long petId, LocalDate from, LocalDate to);
}

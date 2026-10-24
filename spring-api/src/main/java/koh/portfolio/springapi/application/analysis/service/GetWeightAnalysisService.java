package koh.portfolio.springapi.application.analysis.service;

import koh.portfolio.springapi.application.analysis.usecase.GetWeightAnalysisUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.analysis.exception.AnalysisErrorCode;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;
import koh.portfolio.springapi.domain.analysis.port.AnalysisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetWeightAnalysisService implements GetWeightAnalysisUseCase {
    private final AnalysisPetChecker analysisPetChecker;
    private final AnalysisClient analysisClient;

    @Override
    public WeightAnalysis execute(Long userId, Long petId, LocalDate from, LocalDate to) {
        analysisPetChecker.checkOwner(userId, petId);
        Preconditions.validate(from == null || to == null || !from.isAfter(to), AnalysisErrorCode.INVALID_PERIOD);

        return analysisClient.getWeight(petId, from, to);
    }
}

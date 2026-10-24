package koh.portfolio.springapi.presentation.analysis;

import koh.portfolio.springapi.application.analysis.usecase.GetHealthScoreUseCase;
import koh.portfolio.springapi.application.analysis.usecase.GetVaccinationForecastUseCase;
import koh.portfolio.springapi.application.analysis.usecase.GetWeightAnalysisUseCase;
import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.analysis.model.HealthScore;
import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// 分析API（07 §7）。所有者チェックの後、分析API（FastAPI）の内部APIへ中継する。
// 症状統計（07 §7-3 symptoms/summary）は練習用にあえて未実装（TODO.md）
@RestController
@RequestMapping("/api/v1/analysis/pets/{petId}")
@RequiredArgsConstructor
public class AnalysisController {
    private final GetWeightAnalysisUseCase getWeightAnalysisUseCase;
    private final GetHealthScoreUseCase getHealthScoreUseCase;
    private final GetVaccinationForecastUseCase getVaccinationForecastUseCase;

    @GetMapping("/weight")
    public ResponseEntity<ApiResponse<WeightAnalysis>> getWeight(
            Authentication authentication,
            @PathVariable Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getWeightAnalysisUseCase.execute(userId, petId, from, to)));
    }

    @GetMapping("/health-score")
    public ResponseEntity<ApiResponse<HealthScore>> getHealthScore(
            Authentication authentication,
            @PathVariable Long petId
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getHealthScoreUseCase.execute(userId, petId)));
    }

    @GetMapping("/vaccination/next")
    public ResponseEntity<ApiResponse<VaccinationForecast>> getVaccinationForecast(
            Authentication authentication,
            @PathVariable Long petId
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(getVaccinationForecastUseCase.execute(userId, petId)));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw AuthErrorCode.AUTH_FAILED.defaultException();
        }
        return userId;
    }
}

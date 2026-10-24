package koh.portfolio.springapi.application.analysis.service;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.analysis.exception.AnalysisErrorCode;
import koh.portfolio.springapi.domain.analysis.model.HealthScore;
import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;
import koh.portfolio.springapi.domain.analysis.port.AnalysisClient;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private AnalysisClient analysisClient;

    private GetWeightAnalysisService getWeightAnalysisService;
    private GetHealthScoreService getHealthScoreService;
    private GetVaccinationForecastService getVaccinationForecastService;

    private final Long userId = 1L;
    private final Long petId = 10L;
    private final LocalDate from = LocalDate.of(2026, 10, 1);
    private final LocalDate to = LocalDate.of(2026, 10, 25);

    @BeforeEach
    void setUp() {
        AnalysisPetChecker checker = new AnalysisPetChecker(petRepository);
        getWeightAnalysisService = new GetWeightAnalysisService(checker, analysisClient);
        getHealthScoreService = new GetHealthScoreService(checker, analysisClient);
        getVaccinationForecastService = new GetVaccinationForecastService(checker, analysisClient);
    }

    private Pet pet(Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return new Pet(petId, ownerId, "Momo", PetType.CAT, LocalDate.of(2023, 1, 1), PetGender.FEMALE,
                new BigDecimal("4.50"), null, false, null, now, now, null);
    }

    private void assertErrorCode(ThrowingCallable call, AnalysisErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode().code()).isEqualTo(errorCode.code()));
    }

    @Test
    @DisplayName("体重推移: 飼い主本人のペットなら、期間を付けて分析APIの結果を返す")
    void get_weight_success() {
        WeightAnalysis analysis = new WeightAnalysis(List.of(), null);
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        when(analysisClient.getWeight(petId, from, to)).thenReturn(analysis);

        assertThat(getWeightAnalysisService.execute(userId, petId, from, to)).isSameAs(analysis);
    }

    @Test
    @DisplayName("体重推移: 開始日と終了日が同じ日、または片方だけの指定は許可する")
    void get_weight_accepts_same_day_and_open_period() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));

        getWeightAnalysisService.execute(userId, petId, from, from);
        getWeightAnalysisService.execute(userId, petId, from, null);
        getWeightAnalysisService.execute(userId, petId, null, null);

        verify(analysisClient).getWeight(petId, from, from);
        verify(analysisClient).getWeight(petId, from, null);
        verify(analysisClient).getWeight(petId, null, null);
    }

    @Test
    @DisplayName("体重推移: 開始日が終了日より後ならANALYSIS-002で、分析APIを呼ばない")
    void get_weight_fail_when_period_reversed() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));

        assertErrorCode(() -> getWeightAnalysisService.execute(userId, petId, to, from), AnalysisErrorCode.INVALID_PERIOD);
        verifyNoInteractions(analysisClient);
    }

    @Test
    @DisplayName("体重推移: 他ユーザーのペットはANALYSIS-001で、分析APIを呼ばない")
    void get_weight_fail_when_not_owner() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        assertErrorCode(() -> getWeightAnalysisService.execute(userId, petId, from, to), AnalysisErrorCode.NOT_PET_OWNER);
        verifyNoInteractions(analysisClient);
    }

    @Test
    @DisplayName("体重推移: 存在しない（削除済みを含む）ペットはANALYSIS-001")
    void get_weight_fail_when_pet_missing() {
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertErrorCode(() -> getWeightAnalysisService.execute(userId, petId, null, null), AnalysisErrorCode.NOT_PET_OWNER);
        verifyNoInteractions(analysisClient);
    }

    @Test
    @DisplayName("健康スコア: 飼い主本人のペットなら分析APIの結果を返す")
    void get_health_score_success() {
        HealthScore score = new HealthScore(100, "GOOD", new HealthScore.Period(from, to), List.of());
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        when(analysisClient.getHealthScore(petId)).thenReturn(score);

        assertThat(getHealthScoreService.execute(userId, petId)).isSameAs(score);
    }

    @Test
    @DisplayName("健康スコア: 他ユーザーのペットはANALYSIS-001で、分析APIを呼ばない")
    void get_health_score_fail_when_not_owner() {
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(999L)));

        assertErrorCode(() -> getHealthScoreService.execute(userId, petId), AnalysisErrorCode.NOT_PET_OWNER);
        verifyNoInteractions(analysisClient);
    }

    @Test
    @DisplayName("次回接種推奨日: 飼い主本人のペットなら分析APIの結果を返す")
    void get_vaccination_forecast_success() {
        VaccinationForecast forecast = new VaccinationForecast(List.of());
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet(userId)));
        when(analysisClient.getVaccinationForecast(petId)).thenReturn(forecast);

        assertThat(getVaccinationForecastService.execute(userId, petId)).isSameAs(forecast);
    }

    @Test
    @DisplayName("次回接種推奨日: 存在しないペットはANALYSIS-001で、分析APIを呼ばない")
    void get_vaccination_forecast_fail_when_pet_missing() {
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertErrorCode(() -> getVaccinationForecastService.execute(userId, petId), AnalysisErrorCode.NOT_PET_OWNER);
        verifyNoInteractions(analysisClient);
    }
}

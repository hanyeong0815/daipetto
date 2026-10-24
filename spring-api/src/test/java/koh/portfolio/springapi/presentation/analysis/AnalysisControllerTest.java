package koh.portfolio.springapi.presentation.analysis;

import koh.portfolio.springapi.application.analysis.usecase.GetHealthScoreUseCase;
import koh.portfolio.springapi.application.analysis.usecase.GetVaccinationForecastUseCase;
import koh.portfolio.springapi.application.analysis.usecase.GetWeightAnalysisUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.analysis.exception.AnalysisErrorCode;
import koh.portfolio.springapi.domain.analysis.model.HealthScore;
import koh.portfolio.springapi.domain.analysis.model.VaccinationForecast;
import koh.portfolio.springapi.domain.analysis.model.WeightAnalysis;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AnalysisController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetWeightAnalysisUseCase getWeightAnalysisUseCase;

    @MockBean
    private GetHealthScoreUseCase getHealthScoreUseCase;

    @MockBean
    private GetVaccinationForecastUseCase getVaccinationForecastUseCase;

    private final UsernamePasswordAuthenticationToken user =
            new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @Test
    @DisplayName("体重推移: 期間を受け取り、分析結果をApiResponse形式で返す")
    void get_weight_success() throws Exception {
        WeightAnalysis analysis = new WeightAnalysis(
                List.of(new WeightAnalysis.Point(LocalDate.of(2026, 10, 1), new BigDecimal("4.30"))),
                new WeightAnalysis.Summary(1, new BigDecimal("4.30"), new BigDecimal("4.30"), new BigDecimal("4.30"),
                        new BigDecimal("4.30"), new BigDecimal("0.00"), new BigDecimal("0.0")));
        when(getWeightAnalysisUseCase.execute(1L, 10L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 25)))
                .thenReturn(analysis);

        mockMvc.perform(get("/api/v1/analysis/pets/10/weight?from=2026-10-01&to=2026-10-25").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.points[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.data.points[0].weight").value(4.30))
                .andExpect(jsonPath("$.data.summary.count").value(1))
                .andExpect(jsonPath("$.data.summary.changeRate").value(0.0));
    }

    @Test
    @DisplayName("体重推移: 期間を省略すると null を渡す")
    void get_weight_without_period() throws Exception {
        when(getWeightAnalysisUseCase.execute(1L, 10L, null, null)).thenReturn(new WeightAnalysis(List.of(), null));

        mockMvc.perform(get("/api/v1/analysis/pets/10/weight").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points").isEmpty())
                .andExpect(jsonPath("$.data.summary").doesNotExist());

        verify(getWeightAnalysisUseCase).execute(1L, 10L, null, null);
    }

    @Test
    @DisplayName("体重推移: 日付の形式が不正ならVALIDATION-001")
    void get_weight_fail_when_date_invalid() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/pets/10/weight?from=2026/10/01").principal(user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));

        verifyNoInteractions(getWeightAnalysisUseCase);
    }

    @Test
    @DisplayName("体重推移: 期間の前後が不正ならANALYSIS-002（400）")
    void get_weight_fail_when_period_reversed() throws Exception {
        when(getWeightAnalysisUseCase.execute(eq(1L), eq(10L), any(), any()))
                .thenThrow(AnalysisErrorCode.INVALID_PERIOD.defaultException());

        mockMvc.perform(get("/api/v1/analysis/pets/10/weight?from=2026-10-25&to=2026-10-01").principal(user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ANALYSIS-002"));
    }

    @Test
    @DisplayName("健康スコア: 分析結果を返す")
    void get_health_score_success() throws Exception {
        HealthScore score = new HealthScore(75, "CAUTION",
                new HealthScore.Period(LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 25)),
                List.of(new HealthScore.Factor("WEIGHT_CHANGE", true, 15, new BigDecimal("6.2")),
                        new HealthScore.Factor("SYMPTOM_FREQUENCY", true, 10, new BigDecimal("1"))));
        when(getHealthScoreUseCase.execute(1L, 10L)).thenReturn(score);

        mockMvc.perform(get("/api/v1/analysis/pets/10/health-score").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(75))
                .andExpect(jsonPath("$.data.grade").value("CAUTION"))
                .andExpect(jsonPath("$.data.period.from").value("2026-09-26"))
                .andExpect(jsonPath("$.data.factors[0].penalty").value(15))
                .andExpect(jsonPath("$.data.factors[1].value").value(1));
    }

    @Test
    @DisplayName("健康スコア: 他ユーザーのペットはANALYSIS-001（403）")
    void get_health_score_fail_when_not_owner() throws Exception {
        when(getHealthScoreUseCase.execute(1L, 10L)).thenThrow(AnalysisErrorCode.NOT_PET_OWNER.defaultException());

        mockMvc.perform(get("/api/v1/analysis/pets/10/health-score").principal(user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ANALYSIS-001"));
    }

    @Test
    @DisplayName("次回接種推奨日: 分析結果を返す")
    void get_vaccination_forecast_success() throws Exception {
        VaccinationForecast forecast = new VaccinationForecast(List.of(new VaccinationForecast.Item(
                "狂犬病", LocalDate.of(2025, 11, 1), LocalDate.of(2026, 11, 1), "RECOMMENDED", null, false, 7L)));
        when(getVaccinationForecastUseCase.execute(1L, 10L)).thenReturn(forecast);

        mockMvc.perform(get("/api/v1/analysis/pets/10/vaccination/next").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].vaccineName").value("狂犬病"))
                .andExpect(jsonPath("$.data.items[0].nextDate").value("2026-11-01"))
                .andExpect(jsonPath("$.data.items[0].daysUntil").value(7));
    }

    @Test
    @DisplayName("次回接種推奨日: 分析サービスに接続できなければANALYSIS-003（503）")
    void get_vaccination_forecast_fail_when_unavailable() throws Exception {
        when(getVaccinationForecastUseCase.execute(1L, 10L))
                .thenThrow(AnalysisErrorCode.ANALYSIS_UNAVAILABLE.defaultException());

        mockMvc.perform(get("/api/v1/analysis/pets/10/vaccination/next").principal(user))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ANALYSIS-003"))
                .andExpect(jsonPath("$.message").value("分析サービスを利用できません。時間をおいて再度お試しください。"));
    }

    @Test
    @DisplayName("症状統計は練習用に未実装のため存在しないURL（COMMON-001）")
    void symptoms_summary_is_not_routed() throws Exception {
        mockMvc.perform(get("/api/v1/analysis/pets/10/symptoms/summary").principal(user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }
}

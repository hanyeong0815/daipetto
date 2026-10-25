package koh.portfolio.springapi.presentation.vaccination;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationRequest;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationResponse;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.VaccinationResponse;
import koh.portfolio.springapi.application.vaccination.usecase.CancelVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.CompleteVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.CreateVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.DeleteVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.GetVaccinationListUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.RescheduleVaccinationUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.vaccination.exception.VaccinationErrorCode;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = VaccinationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class VaccinationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateVaccinationUseCase createVaccinationUseCase;

    @MockitoBean
    private GetVaccinationListUseCase getVaccinationListUseCase;

    @MockitoBean
    private CompleteVaccinationUseCase completeVaccinationUseCase;

    @MockitoBean
    private RescheduleVaccinationUseCase rescheduleVaccinationUseCase;

    @MockitoBean
    private CancelVaccinationUseCase cancelVaccinationUseCase;

    @MockitoBean
    private DeleteVaccinationUseCase deleteVaccinationUseCase;

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    @DisplayName("POST /api/v1/pets/{petId}/vaccinations - 接種予定登録成功")
    void create_vaccination_success() throws Exception {
        // given
        when(createVaccinationUseCase.execute(eq(1L), eq(10L), any(CreateVaccinationRequest.class)))
                .thenReturn(CreateVaccinationResponse.builder().vaccinationId(100L).build());

        // when & then
        mockMvc.perform(post("/api/v1/pets/10/vaccinations")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateVaccinationRequest("混合ワクチン", LocalDate.of(2026, 10, 10)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vaccinationId").value(100));
    }

    @Test
    @DisplayName("POST /api/v1/pets/{petId}/vaccinations - ワクチン名・接種予定日が無い場合Validation失敗")
    void create_vaccination_validation_fail() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/pets/10/vaccinations")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateVaccinationRequest(" ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));

        verifyNoInteractions(createVaccinationUseCase);
    }

    @Test
    @DisplayName("GET /api/v1/pets/{petId}/vaccinations - 接種予定一覧取得成功")
    void get_vaccination_list_success() throws Exception {
        // given
        when(getVaccinationListUseCase.execute(1L, 10L)).thenReturn(List.of(
                VaccinationResponse.builder()
                        .vaccinationId(100L)
                        .vaccineName("混合ワクチン")
                        .scheduledDate(LocalDate.of(2026, 10, 10))
                        .status(VaccinationStatus.SCHEDULED)
                        .build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/pets/10/vaccinations").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].vaccinationId").value(100))
                .andExpect(jsonPath("$.data[0].scheduledDate").value("2026-10-10"))
                .andExpect(jsonPath("$.data[0].completedDate").doesNotExist())
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/vaccinations/{id}/complete - body省略時は完了日nullでUseCaseを呼ぶ")
    void complete_vaccination_without_body() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/vaccinations/100/complete").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(completeVaccinationUseCase).execute(eq(1L), eq(100L), isNull());
    }

    @Test
    @DisplayName("PATCH /api/v1/vaccinations/{id}/complete - 未来の接種完了日はValidation失敗")
    void complete_vaccination_validation_fail_when_future_date() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/vaccinations/100/complete")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completedDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));

        verifyNoInteractions(completeVaccinationUseCase);
    }

    @Test
    @DisplayName("PATCH /api/v1/vaccinations/{id}/complete - 不正な状態遷移は409とVACCINATION-003を返す")
    void complete_vaccination_conflict() throws Exception {
        // given
        doThrow(VaccinationErrorCode.INVALID_STATE_TRANSITION.defaultException())
                .when(completeVaccinationUseCase).execute(eq(1L), eq(100L), any());

        // when & then
        mockMvc.perform(patch("/api/v1/vaccinations/100/complete").principal(auth(1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VACCINATION-003"));
    }

    @Test
    @DisplayName("PATCH /api/v1/vaccinations/{id}/reschedule - 再登録成功")
    void reschedule_vaccination_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/vaccinations/100/reschedule")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledDate\":\"2026-11-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(rescheduleVaccinationUseCase).execute(1L, 100L, LocalDate.of(2026, 11, 1));
    }

    @Test
    @DisplayName("PATCH /api/v1/vaccinations/{id}/reschedule - 接種予定日が無い場合Validation失敗")
    void reschedule_vaccination_validation_fail() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/vaccinations/100/reschedule")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));

        verifyNoInteractions(rescheduleVaccinationUseCase);
    }

    @Test
    @DisplayName("PATCH /api/v1/vaccinations/{id}/cancel - 中止成功")
    void cancel_vaccination_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/vaccinations/100/cancel").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cancelVaccinationUseCase).execute(1L, 100L);
    }

    @Test
    @DisplayName("PATCH /api/v1/vaccinations/{id}/cancel - 不正な状態遷移は409とVACCINATION-003を返す")
    void cancel_vaccination_conflict() throws Exception {
        // given
        doThrow(VaccinationErrorCode.INVALID_STATE_TRANSITION.defaultException())
                .when(cancelVaccinationUseCase).execute(1L, 100L);

        // when & then
        mockMvc.perform(patch("/api/v1/vaccinations/100/cancel").principal(auth(1L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VACCINATION-003"));
    }

    @Test
    @DisplayName("DELETE /api/v1/vaccinations/{id} - 削除成功")
    void delete_vaccination_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/vaccinations/100").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(deleteVaccinationUseCase).execute(1L, 100L);
    }

    @Test
    @DisplayName("DELETE /api/v1/vaccinations/{id} - 他ユーザーのペットは403とVACCINATION-002を返す")
    void delete_vaccination_forbidden() throws Exception {
        // given
        doThrow(VaccinationErrorCode.NOT_PET_OWNER.defaultException())
                .when(deleteVaccinationUseCase).execute(1L, 100L);

        // when & then
        mockMvc.perform(delete("/api/v1/vaccinations/100").principal(auth(1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VACCINATION-002"));
    }
}

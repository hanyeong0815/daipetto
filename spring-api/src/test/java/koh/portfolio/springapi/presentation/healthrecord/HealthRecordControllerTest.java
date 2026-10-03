package koh.portfolio.springapi.presentation.healthrecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.HealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.UpdateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.usecase.CreateHealthRecordUseCase;
import koh.portfolio.springapi.application.healthrecord.usecase.DeleteHealthRecordUseCase;
import koh.portfolio.springapi.application.healthrecord.usecase.GetHealthRecordListUseCase;
import koh.portfolio.springapi.application.healthrecord.usecase.UpdateHealthRecordUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HealthRecordController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HealthRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateHealthRecordUseCase createHealthRecordUseCase;

    @MockBean
    private GetHealthRecordListUseCase getHealthRecordListUseCase;

    @MockBean
    private UpdateHealthRecordUseCase updateHealthRecordUseCase;

    @MockBean
    private DeleteHealthRecordUseCase deleteHealthRecordUseCase;

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    @DisplayName("POST /api/v1/pets/{petId}/health-records - 健康記録登録成功")
    void create_health_record_success() throws Exception {
        // given
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(
                new BigDecimal("4.70"), "咳, 食欲低下", "少し元気がない", LocalDate.of(2026, 5, 20));

        when(createHealthRecordUseCase.execute(eq(1L), eq(10L), any(CreateHealthRecordRequest.class)))
                .thenReturn(CreateHealthRecordResponse.builder().healthRecordId(100L).build());

        // when & then
        mockMvc.perform(post("/api/v1/pets/10/health-records")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.healthRecordId").value(100));
    }

    @Test
    @DisplayName("HEALTH-T003: POST /api/v1/pets/{petId}/health-records - 体重が負数の場合Validation失敗")
    void create_health_record_validation_fail_when_weight_negative() throws Exception {
        // given
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(
                new BigDecimal("-1.00"), null, null, null);

        // when & then
        mockMvc.perform(post("/api/v1/pets/10/health-records")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    @Test
    @DisplayName("GET /api/v1/pets/{petId}/health-records - 健康記録一覧取得成功")
    void get_health_record_list_success() throws Exception {
        // given
        when(getHealthRecordListUseCase.execute(1L, 10L)).thenReturn(List.of(
                HealthRecordResponse.builder()
                        .healthRecordId(100L)
                        .weight(new BigDecimal("4.70"))
                        .symptom("咳")
                        .memo("少し元気がない")
                        .recordedDate(LocalDate.of(2026, 5, 20))
                        .build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/pets/10/health-records").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].healthRecordId").value(100))
                .andExpect(jsonPath("$.data[0].symptom").value("咳"))
                .andExpect(jsonPath("$.data[0].recordedDate").value("2026-05-20"));
    }

    @Test
    @DisplayName("PATCH /api/v1/health-records/{healthRecordId} - 健康記録更新成功")
    void update_health_record_success() throws Exception {
        // given
        UpdateHealthRecordRequest request = new UpdateHealthRecordRequest(
                new BigDecimal("5.10"), "食欲回復", "元気になった", LocalDate.of(2026, 5, 25));

        // when & then
        mockMvc.perform(patch("/api/v1/health-records/100")
                        .principal(auth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(updateHealthRecordUseCase).execute(eq(1L), eq(100L), any(UpdateHealthRecordRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/health-records/{healthRecordId} - 健康記録論理削除成功")
    void delete_health_record_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/health-records/100").principal(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(deleteHealthRecordUseCase).execute(1L, 100L);
    }
}

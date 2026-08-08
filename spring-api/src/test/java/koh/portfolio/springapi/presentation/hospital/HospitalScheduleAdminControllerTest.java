package koh.portfolio.springapi.presentation.hospital;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleResponse;
import koh.portfolio.springapi.application.hospital.usecase.BlockHospitalScheduleUseCase;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalScheduleUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UnblockHospitalScheduleUseCase;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HospitalScheduleAdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HospitalScheduleAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateHospitalScheduleUseCase createHospitalScheduleUseCase;

    @MockBean
    private BlockHospitalScheduleUseCase blockHospitalScheduleUseCase;

    @MockBean
    private UnblockHospitalScheduleUseCase unblockHospitalScheduleUseCase;

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("POST /api/v1/admin/hospitals/{hospitalId}/schedules - 予約枠登録成功")
    void create_hospital_schedule_success() throws Exception {
        // given
        CreateHospitalScheduleRequest request = new CreateHospitalScheduleRequest(
                LocalDate.of(2026, 5, 20), LocalTime.of(10, 0), LocalTime.of(10, 30)
        );

        when(createHospitalScheduleUseCase.execute(eq(1L), any(CreateHospitalScheduleRequest.class)))
                .thenReturn(new CreateHospitalScheduleResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/admin/hospitals/{hospitalId}/schedules - availableDateがnullの場合Validation失敗")
    void create_hospital_schedule_validation_fail_when_available_date_null() throws Exception {
        // given
        CreateHospitalScheduleRequest request = new CreateHospitalScheduleRequest(
                null, LocalTime.of(10, 0), LocalTime.of(10, 30)
        );

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    // ------------------------------------------------------------------- block

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{hospitalId}/schedules/{scheduleId}/block - BLOCKED設定成功")
    void block_hospital_schedule_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/1/schedules/10/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(blockHospitalScheduleUseCase).execute(1L, 10L);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{hospitalId}/schedules/{scheduleId}/unblock - AVAILABLE設定成功")
    void unblock_hospital_schedule_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/1/schedules/10/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(unblockHospitalScheduleUseCase).execute(1L, 10L);
    }
}

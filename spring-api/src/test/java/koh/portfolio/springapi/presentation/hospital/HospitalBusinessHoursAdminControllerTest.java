package koh.portfolio.springapi.presentation.hospital;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.UpdateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalBusinessHoursUseCase;
import koh.portfolio.springapi.application.hospital.usecase.DeleteHospitalBusinessHoursUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UpdateHospitalBusinessHoursUseCase;
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

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HospitalBusinessHoursAdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HospitalBusinessHoursAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateHospitalBusinessHoursUseCase createHospitalBusinessHoursUseCase;

    @MockBean
    private UpdateHospitalBusinessHoursUseCase updateHospitalBusinessHoursUseCase;

    @MockBean
    private DeleteHospitalBusinessHoursUseCase deleteHospitalBusinessHoursUseCase;

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("POST /api/v1/admin/hospitals/{hospitalId}/business-hours - 営業時間登録成功")
    void create_hospital_business_hours_success() throws Exception {
        // given
        CreateHospitalBusinessHoursRequest request = new CreateHospitalBusinessHoursRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0), 30
        );

        when(createHospitalBusinessHoursUseCase.execute(eq(1L), any(CreateHospitalBusinessHoursRequest.class)))
                .thenReturn(new CreateHospitalBusinessHoursResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals/1/business-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.businessHoursId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/admin/hospitals/{hospitalId}/business-hours - dayOfWeekがnullの場合Validation失敗")
    void create_hospital_business_hours_validation_fail_when_day_of_week_null() throws Exception {
        // given
        CreateHospitalBusinessHoursRequest request = new CreateHospitalBusinessHoursRequest(
                null, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, 30
        );

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals/1/business-hours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    // ------------------------------------------------------------------ update

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId} - 営業時間更新成功")
    void update_hospital_business_hours_success() throws Exception {
        // given
        UpdateHospitalBusinessHoursRequest request = new UpdateHospitalBusinessHoursRequest(
                LocalTime.of(10, 0), LocalTime.of(19, 0), LocalTime.of(12, 0), LocalTime.of(13, 0), 20
        );

        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/1/business-hours/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(updateHospitalBusinessHoursUseCase).execute(eq(1L), eq(10L), any(UpdateHospitalBusinessHoursRequest.class));
    }

    // ------------------------------------------------------------------ delete

    @Test
    @DisplayName("DELETE /api/v1/admin/hospitals/{hospitalId}/business-hours/{businessHoursId} - 営業時間削除成功")
    void delete_hospital_business_hours_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/admin/hospitals/1/business-hours/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(deleteHospitalBusinessHoursUseCase).execute(1L, 10L);
    }
}

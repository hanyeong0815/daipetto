package koh.portfolio.springapi.presentation.hospital;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.HospitalScheduleResponse;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalScheduleListUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HospitalScheduleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HospitalScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetHospitalScheduleListUseCase getHospitalScheduleListUseCase;

    @Test
    @DisplayName("GET /api/v1/hospitals/{hospitalId}/schedules - 病院予約枠一覧取得成功")
    void get_hospital_schedule_list_success() throws Exception {
        // given
        List<HospitalScheduleResponse> schedules = List.of(
                HospitalScheduleResponse.builder()
                        .scheduleId(1L)
                        .availableDate(LocalDate.of(2026, 5, 20))
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(10, 30))
                        .status(HospitalScheduleStatus.AVAILABLE)
                        .build()
        );

        when(getHospitalScheduleListUseCase.execute(1L)).thenReturn(schedules);

        // when & then
        mockMvc.perform(get("/api/v1/hospitals/1/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].scheduleId").value(1))
                .andExpect(jsonPath("$.data[0].availableDate").value("2026-05-20"))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
    }
}

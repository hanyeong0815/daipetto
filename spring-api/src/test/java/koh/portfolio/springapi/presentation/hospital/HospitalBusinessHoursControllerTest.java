package koh.portfolio.springapi.presentation.hospital;

import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.HospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalBusinessHoursListUseCase;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HospitalBusinessHoursController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HospitalBusinessHoursControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetHospitalBusinessHoursListUseCase getHospitalBusinessHoursListUseCase;

    @Test
    @DisplayName("GET /api/v1/hospitals/{hospitalId}/business-hours - 病院営業時間一覧取得成功")
    void get_hospital_business_hours_list_success() throws Exception {
        // given
        List<HospitalBusinessHoursResponse> businessHours = List.of(
                HospitalBusinessHoursResponse.builder()
                        .businessHoursId(1L)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .openTime(LocalTime.of(9, 0))
                        .closeTime(LocalTime.of(18, 0))
                        .breakStartTime(LocalTime.of(12, 0))
                        .breakEndTime(LocalTime.of(13, 0))
                        .slotDurationMinutes(30)
                        .build()
        );

        when(getHospitalBusinessHoursListUseCase.execute(1L)).thenReturn(businessHours);

        // when & then
        mockMvc.perform(get("/api/v1/hospitals/1/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].businessHoursId").value(1))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.data[0].slotDurationMinutes").value(30));
    }
}

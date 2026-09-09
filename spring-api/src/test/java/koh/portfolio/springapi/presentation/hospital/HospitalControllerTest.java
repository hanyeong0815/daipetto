package koh.portfolio.springapi.presentation.hospital;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalDetailResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalSummary;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalDetailUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalListUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HospitalController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetHospitalListUseCase getHospitalListUseCase;

    @MockBean
    private GetHospitalDetailUseCase getHospitalDetailUseCase;

    // ---------------------------------------------------------------- getList

    @Test
    @DisplayName("GET /api/v1/hospitals - 病院一覧取得成功")
    void get_hospital_list_success() throws Exception {
        // given
        List<HospitalSummary> summaries = List.of(
                new HospitalSummary(1L, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678")
        );

        when(getHospitalListUseCase.execute("Tokyo", "Shibuya")).thenReturn(summaries);

        // when & then
        mockMvc.perform(get("/api/v1/hospitals").param("keyword", "Tokyo").param("area", "Shibuya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Tokyo Animal Hospital"))
                .andExpect(jsonPath("$.data[0].address").value("Tokyo, Shibuya"));
    }

    // -------------------------------------------------------------- getDetail

    @Test
    @DisplayName("GET /api/v1/hospitals/{hospitalId} - 病院詳細取得成功")
    void get_hospital_detail_success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        HospitalDetailResponse response = new HospitalDetailResponse(
                1L, "Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678", HospitalStatus.ACTIVE, now
        );

        when(getHospitalDetailUseCase.execute(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Tokyo Animal Hospital"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}

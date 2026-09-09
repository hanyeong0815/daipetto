package koh.portfolio.springapi.presentation.hospital;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.UpdateHospitalRequest;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.SuspendHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UpdateHospitalUseCase;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HospitalAdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HospitalAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateHospitalUseCase createHospitalUseCase;

    @MockBean
    private UpdateHospitalUseCase updateHospitalUseCase;

    @MockBean
    private SuspendHospitalUseCase suspendHospitalUseCase;

    @Test
    @DisplayName("POST /api/v1/admin/hospitals - 病院登録成功")
    void create_hospital_success() throws Exception {
        // given
        CreateHospitalRequest request = new CreateHospitalRequest("Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678");

        when(createHospitalUseCase.execute(any(CreateHospitalRequest.class)))
                .thenReturn(new CreateHospitalResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hospitalId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/admin/hospitals - nameが空白の場合Validation失敗")
    void create_hospital_validation_fail_when_name_blank() throws Exception {
        // given
        CreateHospitalRequest request = new CreateHospitalRequest("", "Tokyo, Shibuya", null);

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/hospitals - addressが空白の場合Validation失敗")
    void create_hospital_validation_fail_when_address_blank() throws Exception {
        // given
        CreateHospitalRequest request = new CreateHospitalRequest("Tokyo Animal Hospital", "", null);

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    // ----------------------------------------------------------------- update

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{hospitalId} - 病院情報更新成功")
    void update_hospital_success() throws Exception {
        // given
        UpdateHospitalRequest request = new UpdateHospitalRequest("Tokyo Animal Hospital 2nd", "Tokyo, Shinjuku", "03-9999-9999");

        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(updateHospitalUseCase).execute(eq(1L), any(UpdateHospitalRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{hospitalId} - nameが空白の場合Validation失敗")
    void update_hospital_validation_fail_when_name_blank() throws Exception {
        // given
        UpdateHospitalRequest request = new UpdateHospitalRequest("", "Tokyo, Shinjuku", null);

        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    // ---------------------------------------------------------------- suspend

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{hospitalId}/suspend - 病院停止成功")
    void suspend_hospital_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(suspendHospitalUseCase).execute(1L);
    }
}

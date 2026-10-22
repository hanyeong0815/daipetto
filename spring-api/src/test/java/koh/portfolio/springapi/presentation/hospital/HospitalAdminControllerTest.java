package koh.portfolio.springapi.presentation.hospital;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.UpdateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.HospitalAdminResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.ManagedHospitalResponse;
import koh.portfolio.springapi.application.hospital.usecase.AssignHospitalAdminUseCase;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalAdminsUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetManagedHospitalsUseCase;
import koh.portfolio.springapi.application.hospital.usecase.UnassignHospitalAdminUseCase;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockBean
    private GetManagedHospitalsUseCase getManagedHospitalsUseCase;

    @MockBean
    private GetHospitalAdminsUseCase getHospitalAdminsUseCase;

    @MockBean
    private AssignHospitalAdminUseCase assignHospitalAdminUseCase;

    @MockBean
    private UnassignHospitalAdminUseCase unassignHospitalAdminUseCase;

    @Test
    @DisplayName("POST /api/v1/admin/hospitals - 病院登録成功")
    void create_hospital_success() throws Exception {
        // given
        CreateHospitalRequest request = new CreateHospitalRequest("Tokyo Animal Hospital", "Tokyo, Shibuya", "03-1234-5678");

        when(createHospitalUseCase.execute(any(CreateHospitalRequest.class)))
                .thenReturn(new CreateHospitalResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals").principal(admin())
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
        mockMvc.perform(post("/api/v1/admin/hospitals").principal(admin())
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
        mockMvc.perform(post("/api/v1/admin/hospitals").principal(admin())
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
        mockMvc.perform(patch("/api/v1/admin/hospitals/1").principal(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(updateHospitalUseCase).execute(eq(99L), eq(1L), any(UpdateHospitalRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{hospitalId} - nameが空白の場合Validation失敗")
    void update_hospital_validation_fail_when_name_blank() throws Exception {
        // given
        UpdateHospitalRequest request = new UpdateHospitalRequest("", "Tokyo, Shinjuku", null);

        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/1").principal(admin())
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
        mockMvc.perform(patch("/api/v1/admin/hospitals/1/suspend").principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(suspendHospitalUseCase).execute(1L);
    }


    @Test
    @DisplayName("GET /api/v1/admin/hospitals/mine - 操作者IDで管理対象の病院を返す")
    void get_managed_hospitals_success() throws Exception {
        // given
        when(getManagedHospitalsUseCase.execute(99L)).thenReturn(List.of(ManagedHospitalResponse.builder()
                .hospitalId(1L).name("幸せ動物病院").address("東京都").phoneNumber("03").status(HospitalStatus.SUSPENDED).build()));

        // when & then
        mockMvc.perform(get("/api/v1/admin/hospitals/mine").principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].hospitalId").value(1))
                .andExpect(jsonPath("$.data[0].status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("GET・POST・DELETE /api/v1/admin/hospitals/{id}/admins - 病院管理者の一覧・紐付け・解除")
    void hospital_admin_links() throws Exception {
        // given
        when(getHospitalAdminsUseCase.execute(1L)).thenReturn(List.of(new HospitalAdminResponse(5L, "vet@example.com", "Vet")));

        // when & then
        mockMvc.perform(get("/api/v1/admin/hospitals/1/admins").principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(5));
        mockMvc.perform(post("/api/v1/admin/hospitals/1/admins").principal(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":5}"))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/admin/hospitals/1/admins/5").principal(admin()))
                .andExpect(status().isOk());

        verify(assignHospitalAdminUseCase).execute(1L, 5L);
        verify(unassignHospitalAdminUseCase).execute(1L, 5L);
    }

    @Test
    @DisplayName("POST /api/v1/admin/hospitals/{id}/admins - userIdなしは400、重複は409（HOSPITAL-007）")
    void assign_hospital_admin_errors() throws Exception {
        // given
        doThrow(HospitalErrorCode.HOSPITAL_ADMIN_DUPLICATED.defaultException()).when(assignHospitalAdminUseCase).execute(1L, 6L);

        // when & then
        mockMvc.perform(post("/api/v1/admin/hospitals/1/admins").principal(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        mockMvc.perform(post("/api/v1/admin/hospitals/1/admins").principal(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":6}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HOSPITAL-007"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/hospitals/{id} - 管理対象外の病院は403（HOSPITAL-005）")
    void update_hospital_forbidden_when_not_manager() throws Exception {
        // given
        doThrow(HospitalErrorCode.NOT_HOSPITAL_MANAGER.defaultException())
                .when(updateHospitalUseCase).execute(eq(99L), eq(2L), any(UpdateHospitalRequest.class));

        // when & then
        mockMvc.perform(patch("/api/v1/admin/hospitals/2").principal(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"n\",\"address\":\"a\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOSPITAL-005"));
    }
    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(99L, null, List.of());
    }
}

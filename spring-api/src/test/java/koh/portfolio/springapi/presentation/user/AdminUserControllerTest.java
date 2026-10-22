package koh.portfolio.springapi.presentation.user;

import koh.portfolio.springapi.application.user.dto.AdminUserDto.AdminUserResponse;
import koh.portfolio.springapi.application.user.usecase.ChangeUserRoleUseCase;
import koh.portfolio.springapi.application.user.usecase.ChangeUserStatusUseCase;
import koh.portfolio.springapi.application.user.usecase.SearchUsersUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.common.response.PageResponse;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminUserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchUsersUseCase searchUsersUseCase;

    @MockBean
    private ChangeUserStatusUseCase changeUserStatusUseCase;

    @MockBean
    private ChangeUserRoleUseCase changeUserRoleUseCase;

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - 検索条件とページを渡し、ページ形式で返す")
    void search_users_success() throws Exception {
        // given
        when(searchUsersUseCase.execute("tanaka", Role.ROLE_USER, UserStatus.ACTIVE, 1, 10)).thenReturn(PageResponse.of(
                List.of(new AdminUserResponse(10L, "tanaka@example.com", "Tanaka", Role.ROLE_USER, UserStatus.ACTIVE,
                        LocalDateTime.of(2026, 10, 1, 12, 0))),
                11, 1, 10));

        // when & then
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("keyword", "tanaka").param("role", "ROLE_USER").param("status", "ACTIVE")
                        .param("page", "1").param("size", "10")
                        .principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(10))
                .andExpect(jsonPath("$.data.content[0].email").value("tanaka@example.com"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-10-01T12:00:00+09:00"))
                .andExpect(jsonPath("$.data.totalElements").value(11))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.number").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - 存在しない権限名は500ではなく400（VALIDATION-001）")
    void search_users_rejects_unknown_role() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/admin/users").param("role", "ROLE_ROOT").principal(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));

        verifyNoInteractions(searchUsersUseCase);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/suspend・activate - 操作者IDと対象IDを渡す")
    void suspend_and_activate_user() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/users/10/suspend").principal(admin()))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/users/10/activate").principal(admin()))
                .andExpect(status().isOk());

        verify(changeUserStatusUseCase).execute(1L, 10L, UserStatus.SUSPENDED);
        verify(changeUserStatusUseCase).execute(1L, 10L, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/suspend - 自分自身は400（USER-005）")
    void suspend_self_returns_400() throws Exception {
        // given
        doThrow(UserErrorCode.CANNOT_CHANGE_SELF.defaultException())
                .when(changeUserStatusUseCase).execute(1L, 1L, UserStatus.SUSPENDED);

        // when & then
        mockMvc.perform(patch("/api/v1/admin/users/1/suspend").principal(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER-005"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role - 権限変更成功")
    void change_role_success() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/admin/users/10/role")
                        .principal(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_HOSPITAL_ADMIN\"}"))
                .andExpect(status().isOk());

        verify(changeUserRoleUseCase).execute(1L, 10L, Role.ROLE_HOSPITAL_ADMIN);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role - 権限なし・存在しない権限名は400（VALIDATION-001）")
    void change_role_validation_fail() throws Exception {
        // when & then
        for (String body : List.of("{}", "{\"role\":\"ROLE_ROOT\"}")) {
            mockMvc.perform(patch("/api/v1/admin/users/10/role")
                            .principal(admin())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        verifyNoInteractions(changeUserRoleUseCase);
    }
}

package koh.portfolio.springapi.presentation.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginRequest;
import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginResponse;
import koh.portfolio.springapi.application.auth.dto.LogoutDto.LogoutResponse;
import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenRequest;
import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenResponse;
import koh.portfolio.springapi.application.auth.usecase.LoginUseCase;
import koh.portfolio.springapi.application.auth.usecase.LogoutUseCase;
import koh.portfolio.springapi.application.auth.usecase.RefreshTokenUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.domain.user.model.Role;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginUseCase loginUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private LogoutUseCase logoutUseCase;

    @Test
    @DisplayName("POST /api/v1/auth/login - ログイン成功")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        LoginResponse response = new LoginResponse(
                "access-token",
                "refresh-token",
                Role.ROLE_USER
        );

        when(loginUseCase.execute(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Validation失敗")
    void login_validation_fail() throws Exception {
        LoginRequest request = new LoginRequest(
                "",
                ""
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Token再発行成功")
    void refresh_token_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(
                "current-refresh-token"
        );

        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();

        when(refreshTokenUseCase.execute(any(RefreshTokenRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Validation失敗")
    void refresh_token_validation_fail() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - 成功")
    void logout_success() throws Exception {
        Long userId = 1L;

        LogoutResponse response = new LogoutResponse(true);

        when(logoutUseCase.logout(userId))
                .thenReturn(response);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isLogout").value(true))
                .andExpect(jsonPath("$.message").value("ログアウトしました。"));
    }
}
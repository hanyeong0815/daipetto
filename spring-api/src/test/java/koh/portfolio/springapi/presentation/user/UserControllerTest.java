package koh.portfolio.springapi.presentation.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.auth.usecase.GetMyProfileUseCase;
import koh.portfolio.springapi.application.user.dto.GetMyProfileDto.MyProfileResponse;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserRequest;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserResponse;
import koh.portfolio.springapi.application.user.usecase.RegisterUserUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private GetMyProfileUseCase getMyProfileUseCase;

    @Test
    @DisplayName("POST /api/v1/users - 会員登録成功")
    void register_success() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "test@example.com",
                "password123",
                "hanyeong"
        );

        when(registerUserUseCase.execute(any(RegisterUserRequest.class)))
                .thenReturn(new RegisterUserResponse(1L));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/users - Validation失敗")
    void register_validation_fail() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "",
                "123",
                ""
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me - 自分のプロフィール取得成功")
    void get_my_profile_success() throws Exception {
        // given
        Long userId = 1L;

        MyProfileResponse response = new MyProfileResponse(
                userId,
                "test@example.com",
                "hanyeong",
                Role.ROLE_USER,
                UserStatus.ACTIVE
        );

        when(getMyProfileUseCase.execute(userId)).thenReturn(response);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());

        // when & then
        mockMvc.perform(get("/api/v1/users/me")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("hanyeong"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
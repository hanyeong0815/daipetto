package koh.portfolio.springapi.presentation.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserRequest;
import koh.portfolio.springapi.application.user.dto.RegisterUserDto.RegisterUserResponse;
import koh.portfolio.springapi.application.user.usecase.RegisterUserUseCase;
import koh.portfolio.springapi.common.exception.GlobalExceptionHandler;
import koh.portfolio.springapi.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    @DisplayName("POST /api/v1/users - 会員登録　成功")
    void register_success() throws Exception {
        // given
        RegisterUserRequest request = new RegisterUserRequest(
                "test@example.conm",
                "password123",
                "testName"
        );

        when(registerUserUseCase.execute(any(RegisterUserRequest.class)))
                .thenReturn(new RegisterUserResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/users - validation　失敗")
    void register_validation_fail() throws Exception {
        // given
        RegisterUserRequest request = new RegisterUserRequest(
                "",
                "123",
                ""
        );

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION-001"));
    }
}

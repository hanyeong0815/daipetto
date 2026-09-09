package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginRequest;
import koh.portfolio.springapi.application.auth.dto.LoginDto.LoginResponse;
import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import koh.portfolio.springapi.domain.auth.model.RefreshToken;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginServiceTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtProvider jwtProvider;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtProvider = mock(JwtProvider.class);

        loginService = new LoginService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtProvider
        );
    }

    @Test
    @DisplayName("ログイン成功時、Access Token・Refresh Token・Roleを返却する")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        User user = new User(
                1L,
                "test@example.com",
                "$2a$encoded-password",
                "hanyeong",
                Role.ROLE_USER,
                UserStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        String accessToken = "access-token";
        String refreshTokenValue = "refresh-token";
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusDays(14);

        RefreshToken savedRefreshToken = RefreshToken.issue(
                user.getId(),
                refreshTokenValue,
                refreshTokenExpiresAt
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123", "$2a$encoded-password"))
                .thenReturn(true);

        when(jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        )).thenReturn(accessToken);

        when(jwtProvider.createRefreshToken(user.getId()))
                .thenReturn(refreshTokenValue);

        when(jwtProvider.getRefreshTokenExpiresAt())
                .thenReturn(refreshTokenExpiresAt);

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(savedRefreshToken);

        // when
        LoginResponse response = loginService.execute(request);

        // then
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshTokenValue);
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$encoded-password");
        verify(refreshTokenRepository).revokeAllByUserId(1L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("存在しないメールアドレスの場合、AUTH-001例外が発生する")
    void login_fail_when_email_not_found() {
        // given
        LoginRequest request = new LoginRequest(
                "notfound@example.com",
                "password123"
        );

        when(userRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loginService.execute(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(AuthErrorCode.AUTH_FAILED.code());
                });

        verify(userRepository).findByEmail("notfound@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("パスワードが一致しない場合、AUTH-001例外が発生する")
    void login_fail_when_password_not_matched() {
        // given
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrong-password"
        );

        User user = new User(
                1L,
                "test@example.com",
                "$2a$encoded-password",
                "hanyeong",
                Role.ROLE_USER,
                UserStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "$2a$encoded-password"))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> loginService.execute(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(AuthErrorCode.AUTH_FAILED.code());
                });

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrong-password", "$2a$encoded-password");
        verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("停止中のユーザーはログインできない")
    void login_fail_when_user_suspended() {
        // given
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        User suspendedUser = new User(
                1L,
                "test@example.com",
                "$2a$encoded-password",
                "hanyeong",
                Role.ROLE_USER,
                UserStatus.SUSPENDED,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> loginService.execute(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(AuthErrorCode.SUSPENDED_ACCOUNT.code());
                });

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
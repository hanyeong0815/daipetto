package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenRequest;
import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenResponse;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private JwtProvider jwtProvider;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        jwtProvider = mock(JwtProvider.class);

        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                userRepository,
                jwtProvider
        );
    }

    @Test
    @DisplayName("有効なRefresh Tokenの場合、新しいAccess TokenとRefresh Tokenを返却する")
    void refresh_token_success() {
        // given
        Long userId = 1L;
        String currentRefreshTokenValue = "current-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshTokenValue = "new-refresh-token";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpiresAt = now.plusDays(7);
        LocalDateTime newExpiresAt = now.plusDays(14);

        RefreshToken currentRefreshToken = new RefreshToken(
                1L,
                userId,
                currentRefreshTokenValue,
                currentExpiresAt,
                false,
                now.minusDays(1)
        );

        User user = new User(
                userId,
                "test@example.com",
                "$2a$encoded-password",
                "hanyeong",
                Role.ROLE_USER,
                UserStatus.ACTIVE,
                now.minusDays(10),
                now.minusDays(1),
                null
        );

        RefreshToken savedRefreshToken = RefreshToken.issue(
                userId,
                newRefreshTokenValue,
                newExpiresAt
        );

        when(refreshTokenRepository.findByToken(currentRefreshTokenValue))
                .thenReturn(Optional.of(currentRefreshToken));
        when(jwtProvider.getUserId(currentRefreshTokenValue))
                .thenReturn(userId);
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole()))
                .thenReturn(newAccessToken);
        when(jwtProvider.createRefreshToken(userId))
                .thenReturn(newRefreshTokenValue);
        when(jwtProvider.getRefreshTokenExpiresAt())
                .thenReturn(newExpiresAt);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(savedRefreshToken);

        RefreshTokenRequest request = new RefreshTokenRequest(currentRefreshTokenValue);

        // when
        RefreshTokenResponse response = refreshTokenService.execute(request);

        // then
        assertThat(response.accessToken()).isEqualTo(newAccessToken);
        assertThat(response.refreshToken()).isEqualTo(newRefreshTokenValue);

        verify(refreshTokenRepository).findByToken(currentRefreshTokenValue);
        verify(jwtProvider).getUserId(currentRefreshTokenValue);
        verify(userRepository).findById(userId);
        verify(refreshTokenRepository).revokeByToken(currentRefreshTokenValue);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("存在しないRefresh Tokenの場合、AUTH-003例外が発生する")
    void refresh_token_fail_when_token_not_found() {
        // given
        String requestRefreshToken = "not-found-refresh-token";

        when(refreshTokenRepository.findByToken(requestRefreshToken))
                .thenReturn(Optional.empty());

        RefreshTokenRequest request = new RefreshTokenRequest(requestRefreshToken);

        // when & then
        assertThatThrownBy(() -> refreshTokenService.execute(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN.code());
                });

        verify(refreshTokenRepository).findByToken(requestRefreshToken);
        verify(jwtProvider, never()).getUserId(anyString());
        verify(refreshTokenRepository, never()).revokeByToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("失効済みRefresh Tokenの場合、AUTH-003例外が発生する")
    void refresh_token_fail_when_token_revoked() {
        // given
        String requestRefreshToken = "revoked-refresh-token";
        LocalDateTime now = LocalDateTime.now();

        RefreshToken revokedToken = new RefreshToken(
                1L,
                1L,
                requestRefreshToken,
                now.plusDays(7),
                true,
                now.minusDays(1)
        );

        when(refreshTokenRepository.findByToken(requestRefreshToken))
                .thenReturn(Optional.of(revokedToken));

        RefreshTokenRequest request = new RefreshTokenRequest(requestRefreshToken);

        // when & then
        assertThatThrownBy(() -> refreshTokenService.execute(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN.code());
                });

        verify(refreshTokenRepository).findByToken(requestRefreshToken);
        verify(jwtProvider, never()).getUserId(anyString());
        verify(refreshTokenRepository, never()).revokeByToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("期限切れRefresh Tokenの場合、AUTH-004例外が発生する")
    void refresh_token_fail_when_token_expired() {
        // given
        String requestRefreshToken = "expired-refresh-token";
        LocalDateTime now = LocalDateTime.now();

        RefreshToken expiredToken = new RefreshToken(
                1L,
                1L,
                requestRefreshToken,
                now.minusMinutes(1),
                false,
                now.minusDays(1)
        );

        when(refreshTokenRepository.findByToken(requestRefreshToken))
                .thenReturn(Optional.of(expiredToken));

        RefreshTokenRequest request = new RefreshTokenRequest(requestRefreshToken);

        // when & then
        assertThatThrownBy(() -> refreshTokenService.execute(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(AuthErrorCode.EXPIRED_REFRESH_TOKEN.code());
                });

        verify(refreshTokenRepository).findByToken(requestRefreshToken);
        verify(jwtProvider, never()).getUserId(anyString());
        verify(refreshTokenRepository, never()).revokeByToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("停止中ユーザーの場合、AUTH-002例外が発生する")
    void refresh_token_fail_when_user_suspended() {
        // given
        Long userId = 1L;
        String requestRefreshToken = "valid-refresh-token";
        LocalDateTime now = LocalDateTime.now();

        RefreshToken currentRefreshToken = new RefreshToken(
                1L,
                userId,
                requestRefreshToken,
                now.plusDays(7),
                false,
                now.minusDays(1)
        );

        User suspendedUser = new User(
                userId,
                "test@example.com",
                "$2a$encoded-password",
                "hanyeong",
                Role.ROLE_USER,
                UserStatus.SUSPENDED,
                now.minusDays(10),
                now.minusDays(1),
                null
        );

        when(refreshTokenRepository.findByToken(requestRefreshToken))
                .thenReturn(Optional.of(currentRefreshToken));
        when(jwtProvider.getUserId(requestRefreshToken))
                .thenReturn(userId);
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(suspendedUser));

        RefreshTokenRequest request = new RefreshTokenRequest(requestRefreshToken);

        // when & then
        assertThatThrownBy(() -> refreshTokenService.execute(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode().code())
                            .isEqualTo(AuthErrorCode.SUSPENDED_ACCOUNT.code());
                });

        verify(refreshTokenRepository).findByToken(requestRefreshToken);
        verify(jwtProvider).getUserId(requestRefreshToken);
        verify(userRepository).findById(userId);
        verify(refreshTokenRepository, never()).revokeByToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}

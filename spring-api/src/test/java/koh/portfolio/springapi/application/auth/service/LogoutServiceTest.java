package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.dto.LogoutDto.LogoutResponse;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LogoutServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        logoutService = new LogoutService(refreshTokenRepository, userRepository);
    }

    @Test
    @DisplayName("ログアウト時、ユーザーの有効なRefresh Tokenをすべて失効させる")
    void logout_revokes_all_active_tokens() {
        // given
        Long userId = 1L;

        // when
        LogoutResponse response = logoutService.execute(userId);

        // then
        assertThat(response.isLogout()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    @DisplayName("一括revokeより先にユーザー行を排他取得する（同時refreshが発行する後続Tokenの取りこぼし防止）")
    void logout_locks_user_before_revoking() {
        // given
        Long userId = 1L;

        // when
        logoutService.execute(userId);

        // then
        InOrder inOrder = inOrder(userRepository, refreshTokenRepository);
        inOrder.verify(userRepository).lockForSessionUpdate(userId);
        inOrder.verify(refreshTokenRepository).revokeAllByUserId(userId);
    }
}

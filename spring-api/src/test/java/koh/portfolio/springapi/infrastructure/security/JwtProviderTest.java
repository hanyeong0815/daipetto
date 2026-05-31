package koh.portfolio.springapi.infrastructure.security;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "test-local-dev-only-secret-key-for-jwt-authentication-must-be-long-enough-please-change",
            30,
            14
    );

    @Test
    @DisplayName("Access Tokenを生成し、userIdとroleを取得できる")
    void create_access_token_and_parse_claims() {
        // given
        Long userId = 1L;
        String email = "test@example.com";
        Role role = Role.ROLE_USER;

        // when
        String accessToken = jwtProvider.createAccessToken(userId, email, role);

        // then
        assertThat(accessToken).isNotBlank();
        assertThat(jwtProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtProvider.getUserId(accessToken)).isEqualTo(userId);
        assertThat(jwtProvider.getRole(accessToken)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Access Tokenを生成し、userIdを取得できる")
    void create_refresh_token_and_parse_user_id() {
        // given
        Long userId = 1L;

        // when
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // then
        assertThat(refreshToken).isNotBlank();
        assertThat(jwtProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtProvider.getUserId(refreshToken)).isEqualTo(userId);
    }

    @Test
    @DisplayName("不正なTokenの場合、validateTokenはfalseを返却する")
    void invalid_token_returns_false() {
        // given
        String invalidToken = "invalid-token";

        // when
        boolean result = jwtProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }
}
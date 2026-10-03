package koh.portfolio.springapi.infrastructure.security;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtAuthenticationFilter;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "test-local-dev-only-secret-key-for-jwt-authentication-must-be-long-enough-please-change",
            30,
            14
    );

    private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    @DisplayName("Access Tokenの場合、userIdとroleで認証情報を設定する")
    void authenticates_with_access_token() throws Exception {
        // given
        String accessToken = jwtProvider.createAccessToken(1L, "test@example.com", Role.ROLE_USER);

        // when
        jwtAuthenticationFilter.doFilter(requestWithBearer(accessToken), new MockHttpServletResponse(), new MockFilterChain());

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(1L);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Refresh TokenをBearerに渡した場合、例外を投げず未認証のまま次のフィルターへ進む")
    void does_not_authenticate_with_refresh_token() {
        // given
        String refreshToken = jwtProvider.createRefreshToken(1L);

        // when & then
        assertThatCode(() -> jwtAuthenticationFilter.doFilter(
                requestWithBearer(refreshToken), new MockHttpServletResponse(), new MockFilterChain()))
                .doesNotThrowAnyException();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("不正なTokenの場合、例外を投げず未認証のまま次のフィルターへ進む")
    void does_not_authenticate_with_invalid_token() {
        // when & then
        assertThatCode(() -> jwtAuthenticationFilter.doFilter(
                requestWithBearer("invalid-token"), new MockHttpServletResponse(), new MockFilterChain()))
                .doesNotThrowAnyException();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

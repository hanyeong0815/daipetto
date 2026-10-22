package koh.portfolio.springapi.infrastructure.security;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import koh.portfolio.springapi.domain.user.port.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "test-local-dev-only-secret-key-for-jwt-authentication-must-be-long-enough-please-change",
            30,
            14
    );

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, userRepository);

    private void givenUser(Role role, UserStatus status, LocalDateTime deletedAt) {
        LocalDateTime now = LocalDateTime.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                new User(1L, "test@example.com", "pw", "tester", role, status, now, now, deletedAt)));
    }

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
        givenUser(Role.ROLE_USER, UserStatus.ACTIVE, null);
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

    @Test
    @DisplayName("停止中・論理削除済み・存在しないユーザーは、有効なAccess Tokenでも認証しない（08_State_Design §3-4）")
    void does_not_authenticate_suspended_deleted_or_missing_user() throws Exception {
        String accessToken = jwtProvider.createAccessToken(1L, "test@example.com", Role.ROLE_USER);

        givenUser(Role.ROLE_USER, UserStatus.SUSPENDED, null);
        jwtAuthenticationFilter.doFilter(requestWithBearer(accessToken), new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        givenUser(Role.ROLE_USER, UserStatus.ACTIVE, LocalDateTime.now());
        jwtAuthenticationFilter.doFilter(requestWithBearer(accessToken), new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        jwtAuthenticationFilter.doFilter(requestWithBearer(accessToken), new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("権限はトークン内の値ではなく現在の値で設定する（権限変更が次のリクエストから反映される）")
    void uses_current_role_instead_of_token_claim() throws Exception {
        // given: トークン発行時はSYSTEM_ADMINだったが、現在はUSERに変更済み
        givenUser(Role.ROLE_USER, UserStatus.ACTIVE, null);
        String accessToken = jwtProvider.createAccessToken(1L, "test@example.com", Role.ROLE_SYSTEM_ADMIN);

        // when
        jwtAuthenticationFilter.doFilter(requestWithBearer(accessToken), new MockHttpServletResponse(), new MockFilterChain());

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }
}

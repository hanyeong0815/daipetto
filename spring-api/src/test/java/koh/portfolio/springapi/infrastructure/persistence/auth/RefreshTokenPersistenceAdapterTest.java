package koh.portfolio.springapi.infrastructure.persistence.auth;

import koh.portfolio.springapi.domain.auth.model.RefreshToken;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        RefreshTokenPersistenceAdapter.class,
        RefreshTokenMapperImpl.class
})
class RefreshTokenPersistenceAdapterTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Test
    @DisplayName("ユーザーの有効なRefresh Tokenをすべて失効状態に更新する")
    void revoke_all_active_tokens_by_user_id() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        refreshTokenJpaRepository.save(RefreshTokenEntity.builder()
                .userId(userId)
                .token("token-1")
                .expiresAt(now.plusDays(14))
                .revoked(false)
                .createdAt(now)
                .build());

        refreshTokenJpaRepository.save(RefreshTokenEntity.builder()
                .userId(userId)
                .token("token-2")
                .expiresAt(now.plusDays(14))
                .revoked(false)
                .createdAt(now)
                .build());

        refreshTokenJpaRepository.save(RefreshTokenEntity.builder()
                .userId(999L)
                .token("other-user-token")
                .expiresAt(now.plusDays(14))
                .revoked(false)
                .createdAt(now)
                .build());

        // when
        refreshTokenRepository.revokeAllByUserId(userId);

        // then
        assertThat(refreshTokenJpaRepository.findByToken("token-1"))
                .get()
                .extracting(RefreshTokenEntity::isRevoked)
                .isEqualTo(true);

        assertThat(refreshTokenJpaRepository.findByToken("token-2"))
                .get()
                .extracting(RefreshTokenEntity::isRevoked)
                .isEqualTo(true);

        assertThat(refreshTokenJpaRepository.findByToken("other-user-token"))
                .get()
                .extracting(RefreshTokenEntity::isRevoked)
                .isEqualTo(false);
    }

    @Test
    @DisplayName("Refresh Tokenを保存する")
    void save_refresh_token() {
        // given
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(14);

        RefreshToken refreshToken = RefreshToken.issue(
                1L,
                "refresh-token",
                expiresAt
        );

        // when
        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        // then
        assertThat(savedRefreshToken.getId()).isNotNull();
        assertThat(savedRefreshToken.getUserId()).isEqualTo(1L);
        assertThat(savedRefreshToken.getToken()).isEqualTo("refresh-token");
        assertThat(savedRefreshToken.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("指定したRefresh Tokenのみ失効状態に更新する")
    void revoke_by_token() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        refreshTokenJpaRepository.save(RefreshTokenEntity.builder()
                .userId(userId)
                .token("target-token")
                .expiresAt(now.plusDays(14))
                .revoked(false)
                .createdAt(now)
                .build());

        refreshTokenJpaRepository.save(RefreshTokenEntity.builder()
                .userId(userId)
                .token("other-token")
                .expiresAt(now.plusDays(14))
                .revoked(false)
                .createdAt(now)
                .build());

        // when
        refreshTokenRepository.revokeByToken("target-token");

        // then
        assertThat(refreshTokenJpaRepository.findByToken("target-token"))
                .get()
                .extracting(RefreshTokenEntity::isRevoked)
                .isEqualTo(true);

        assertThat(refreshTokenJpaRepository.findByToken("other-token"))
                .get()
                .extracting(RefreshTokenEntity::isRevoked)
                .isEqualTo(false);
    }
}
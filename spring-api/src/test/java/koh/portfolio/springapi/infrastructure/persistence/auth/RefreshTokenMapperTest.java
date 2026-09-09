package koh.portfolio.springapi.infrastructure.persistence.auth;

import koh.portfolio.springapi.domain.auth.model.RefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenMapperTest {

    private final RefreshTokenMapper refreshTokenMapper =
            Mappers.getMapper(RefreshTokenMapper.class);

    @Test
    @DisplayName("RefreshTokenEntityをDomainに変換する")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(14);

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .id(1L)
                .userId(1L)
                .token("refresh-token")
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(now)
                .build();

        // when
        RefreshToken domain = refreshTokenMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getUserId()).isEqualTo(1L);
        assertThat(domain.getToken()).isEqualTo("refresh-token");
        assertThat(domain.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(domain.isRevoked()).isFalse();
        assertThat(domain.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("RefreshToken DomainをEntityに変換する")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(14);

        RefreshToken domain = new RefreshToken(
                1L,
                1L,
                "refresh-token",
                expiresAt,
                false,
                now
        );

        // when
        RefreshTokenEntity entity = refreshTokenMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(1L);
        assertThat(entity.getToken()).isEqualTo("refresh-token");
        assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entity.isRevoked()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }
}
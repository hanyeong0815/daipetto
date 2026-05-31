package koh.portfolio.springapi.infrastructure.persistence.auth;

import koh.portfolio.springapi.domain.auth.model.RefreshToken;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity savedEntity = refreshTokenJpaRepository.save(refreshTokenMapper.toEntity(refreshToken));

        return refreshTokenMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenJpaRepository.findByToken(token)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        int updatedCount = refreshTokenJpaRepository.revokeAllActiveTokensByUserId(userId);
    }
}

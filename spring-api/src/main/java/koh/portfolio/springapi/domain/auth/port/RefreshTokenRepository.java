package koh.portfolio.springapi.domain.auth.port;

import koh.portfolio.springapi.domain.auth.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    void revokeAllByUserId(Long userId);
}

package koh.portfolio.springapi.domain.auth.port;

import koh.portfolio.springapi.domain.auth.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    int revokeAllByUserId(Long userId);
    // 失効できた件数を返す。Refresh Token rotation の単一消費を保証するために呼び出し側で1件を要求する
    int revokeByToken(String token);
}

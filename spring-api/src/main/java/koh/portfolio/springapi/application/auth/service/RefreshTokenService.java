package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenRequest;
import koh.portfolio.springapi.application.auth.dto.RefreshTokenDto.RefreshTokenResponse;
import koh.portfolio.springapi.application.auth.usecase.RefreshTokenUseCase;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import koh.portfolio.springapi.domain.auth.model.RefreshToken;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import koh.portfolio.springapi.infrastructure.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static koh.portfolio.springapi.common.exception.Preconditions.validate;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public RefreshTokenResponse execute(RefreshTokenRequest request) {
        String requestRefreshToken = request.refreshToken();

        // refreshToken 検索
        RefreshToken currentRefreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(AuthErrorCode.INVALID_REFRESH_TOKEN::defaultException);

        // refreshTokenの有効性確認
        validate(
                !currentRefreshToken.isRevoked(),
                AuthErrorCode.INVALID_REFRESH_TOKEN
        );

        LocalDateTime now = LocalDateTime.now();

        // refreshTokenの満期確認
        validate(
                !currentRefreshToken.isExpired(now),
                AuthErrorCode.EXPIRED_REFRESH_TOKEN
        );

        Long userId = jwtProvider.getUserId(requestRefreshToken);

        // ユーザー情報取得
        User user = userRepository.findById(userId)
                .orElseThrow(AuthErrorCode.AUTH_FAILED::defaultException);

        // 停止ユーザー確認
        validate(
                !user.isSuspended(),
                AuthErrorCode.SUSPENDED_ACCOUNT
        );

        // 既存のrefreshToken無効化
        refreshTokenRepository.revokeByToken(requestRefreshToken);

        // 新たなaccessToken生成
        String newAccessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        // 新たなrefreshToken生成
        String newRefreshTokenValue = jwtProvider.createRefreshToken(user.getId());

        // DB保存用ドメイン作成
        RefreshToken newRefreshToken = RefreshToken.issue(
                user.getId(),
                newRefreshTokenValue,
                jwtProvider.getRefreshTokenExpiresAt()
        );

        // DB保存
        refreshTokenRepository.save(newRefreshToken);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .build();
    }
}

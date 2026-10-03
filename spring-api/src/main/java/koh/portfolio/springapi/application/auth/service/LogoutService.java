package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.dto.LogoutDto.LogoutResponse;
import koh.portfolio.springapi.application.auth.usecase.LogoutUseCase;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LogoutResponse execute(Long userId) {
        // 同時実行中のrefreshが発行する後続Tokenを取りこぼさないよう、
        // 一括revokeの前にユーザー行を排他取得する（REVIEW-002 R-01）
        userRepository.lockForSessionUpdate(userId);

        refreshTokenRepository.revokeAllByUserId(userId);

        return LogoutResponse.builder()
                .isLogout(true)
                .build();
    }
}

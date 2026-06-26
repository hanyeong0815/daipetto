package koh.portfolio.springapi.application.auth.service;

import koh.portfolio.springapi.application.auth.dto.LogoutDto.LogoutResponse;
import koh.portfolio.springapi.application.auth.usecase.LogoutUseCase;
import koh.portfolio.springapi.domain.auth.port.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public LogoutResponse execute(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);

        return LogoutResponse.builder()
                .isLogout(true)
                .build();
    }
}

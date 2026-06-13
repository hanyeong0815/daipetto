package koh.portfolio.springapi.application.auth.usecase;

import koh.portfolio.springapi.application.auth.dto.LogoutDto.LogoutResponse;

public interface LogoutUseCase {
    LogoutResponse logout(Long userId);
}

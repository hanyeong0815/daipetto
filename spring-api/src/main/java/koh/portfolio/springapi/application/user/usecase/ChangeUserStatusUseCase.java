package koh.portfolio.springapi.application.user.usecase;

import koh.portfolio.springapi.domain.user.model.UserStatus;

public interface ChangeUserStatusUseCase {
    void execute(Long adminId, Long userId, UserStatus status);
}

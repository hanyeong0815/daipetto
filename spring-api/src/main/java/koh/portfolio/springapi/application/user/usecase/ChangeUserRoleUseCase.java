package koh.portfolio.springapi.application.user.usecase;

import koh.portfolio.springapi.domain.user.model.Role;

public interface ChangeUserRoleUseCase {
    void execute(Long adminId, Long userId, Role role);
}

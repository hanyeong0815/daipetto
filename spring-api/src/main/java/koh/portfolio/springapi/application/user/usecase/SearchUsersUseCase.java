package koh.portfolio.springapi.application.user.usecase;

import koh.portfolio.springapi.application.user.dto.AdminUserDto.AdminUserResponse;
import koh.portfolio.springapi.common.response.PageResponse;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;

public interface SearchUsersUseCase {
    PageResponse<AdminUserResponse> execute(String keyword, Role role, UserStatus status, int page, int size);
}

package koh.portfolio.springapi.application.user.dto;

import jakarta.validation.constraints.NotNull;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import lombok.Builder;

import java.time.LocalDateTime;

public record AdminUserDto() {
    @Builder
    public record AdminUserResponse(
            Long userId,
            String email,
            String nickname,
            Role role,
            UserStatus status,
            LocalDateTime createdAt
    ) {}

    public record ChangeRoleRequest(
            @NotNull Role role
    ) {}
}

package koh.portfolio.springapi.application.user.dto;

import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.UserStatus;
import lombok.Builder;

public record GetMyProfileDto() {
    @Builder
    public record GetMyProfileResponse(
            Long id,
            String email,
            String nickname,
            Role role,
            UserStatus status
    ) {}
}

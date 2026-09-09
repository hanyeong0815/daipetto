package koh.portfolio.springapi.application.auth.dto;

import lombok.Builder;

public record LogoutDto() {
    @Builder
    public record LogoutResponse(
            boolean isLogout
    ) {}
}

package koh.portfolio.springapi.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public record RefreshTokenDto() {
    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh Tokenを入力してください。")
            String refreshToken
    ) {}

    @Builder
    public record RefreshTokenResponse(
            String accessToken,
            String refreshToken
    ) {}
}

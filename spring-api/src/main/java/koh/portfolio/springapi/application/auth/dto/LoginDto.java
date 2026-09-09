package koh.portfolio.springapi.application.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import koh.portfolio.springapi.domain.user.model.Role;
import lombok.Builder;

public record LoginDto() {
    public record LoginRequest(
            @Email(message = "メールアドレス形式で入力してください。")
            @NotBlank(message = "メールアドレスを入力してください。")
            String email,

            @NotBlank(message = "パスワードを入力してください。")
            String password
    ) {}

    @Builder
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            Role role
    ) {}
}

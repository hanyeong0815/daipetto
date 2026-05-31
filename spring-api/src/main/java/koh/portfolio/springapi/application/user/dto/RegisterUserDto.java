package koh.portfolio.springapi.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public record RegisterUserDto() {
        public record RegisterUserRequest(
                @Email(message = "メールアドレス形式で入力してください。")
                @NotBlank(message = "メールアドレスを入力してください。")
                String email,

                @NotBlank(message = "パスワードを入力してください。")
                @Size(min = 8, max = 100, message = "パスワードは8文字以上100文字以内で入力してください。")
                String password,

                @NotBlank(message = "ニックネームを入力してください。")
                @Size(max = 50, message = "ニックネームは50文字以内で入力してください。")
                String nickname
        ) {}

        @Builder
        public record RegisterUserResponse(
                Long userId
        ) {}
}

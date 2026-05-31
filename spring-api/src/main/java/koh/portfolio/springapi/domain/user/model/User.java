package koh.portfolio.springapi.domain.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class User {

    private Long id;
    private String email;
    private String password;
    private String nickname;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static User register(String email, String encodedPassword, String nickname) {
        LocalDateTime now = LocalDateTime.now();

        return new User(
                null,
                email,
                encodedPassword,
                nickname,
                Role.ROLE_USER,
                UserStatus.ACTIVE,
                now,
                now,
                null
        );
    }

    public boolean isSuspended() {
        return this.status == UserStatus.SUSPENDED;
    }
}
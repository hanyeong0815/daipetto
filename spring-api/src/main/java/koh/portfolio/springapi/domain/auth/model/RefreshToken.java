package koh.portfolio.springapi.domain.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RefreshToken {
    private Long id;
    private Long userId;
    private String token;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;

    public static RefreshToken issue(Long userId, String token, LocalDateTime expiresAt) {
        return new RefreshToken(
                null,
                userId,
                token,
                expiresAt,
                false,
                LocalDateTime.now()
        );
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isAvailable(LocalDateTime now) {
        return !revoked && !isExpired(now);
    }

    public void revoke() {
        this.revoked = true;
    }
}

package koh.portfolio.springapi.domain.auth.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    AUTH_FAILED("AUTH-001", "認証に失敗しました。", HttpStatus.UNAUTHORIZED),
    SUSPENDED_ACCOUNT("AUTH-002", "利用停止中のアカウントです。", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN("AUTH-003", "無効なRefresh Tokenです。", HttpStatus.UNAUTHORIZED),
    EXPIRED_REFRESH_TOKEN("AUTH-004", "期限切れのRefresh Tokenです。", HttpStatus.UNAUTHORIZED),
    DEFAULT("AUTH-999", "認証関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public RuntimeException defaultException() {
        return new AuthException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new AuthException(this, cause);
    }
}

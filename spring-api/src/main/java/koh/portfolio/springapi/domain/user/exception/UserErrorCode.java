package koh.portfolio.springapi.domain.user.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import koh.portfolio.springapi.domain.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    EMAIL_ALREADY_USED("USER-001", "既に登録済みのメールアドレスです。", HttpStatus.CONFLICT),
    VALIDATION_ERROR("USER-002", "入力内容を確認してください。", HttpStatus.BAD_REQUEST),
    NO_SUCH_USER("USER-003", "存在しないユーザーです。", HttpStatus.NOT_FOUND),
    SUSPENDED_USER("USER-004", "利用停止中のアカウントです。", HttpStatus.FORBIDDEN),
    DEFAULT("USER-999", "ユーザー関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

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
        return new UserException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new UserException(this, cause);
    }
}

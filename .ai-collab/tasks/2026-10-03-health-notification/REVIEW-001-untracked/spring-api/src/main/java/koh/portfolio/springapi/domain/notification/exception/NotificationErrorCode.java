package koh.portfolio.springapi.domain.notification.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("NOTIFICATION-001", "存在しない通知です。", HttpStatus.NOT_FOUND),
    NOT_NOTIFICATION_OWNER("NOTIFICATION-002", "通知の受信者本人ではありません。", HttpStatus.FORBIDDEN),
    DEFAULT("NOTIFICATION-999", "通知関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

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
        return new NotificationException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new NotificationException(this, cause);
    }
}

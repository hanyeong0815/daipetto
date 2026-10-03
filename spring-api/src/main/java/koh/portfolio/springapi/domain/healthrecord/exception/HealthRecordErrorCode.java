package koh.portfolio.springapi.domain.healthrecord.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum HealthRecordErrorCode implements ErrorCode {
    HEALTH_RECORD_NOT_FOUND("HEALTH-001", "存在しない健康記録です。", HttpStatus.NOT_FOUND),
    NOT_PET_OWNER("HEALTH-002", "存在しないペット、または他のユーザーのペットです。", HttpStatus.FORBIDDEN),
    DEFAULT("HEALTH-999", "健康記録関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

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
        return new HealthRecordException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new HealthRecordException(this, cause);
    }
}

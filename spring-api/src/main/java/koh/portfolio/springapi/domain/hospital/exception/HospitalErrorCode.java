package koh.portfolio.springapi.domain.hospital.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum HospitalErrorCode implements ErrorCode {
    HOSPITAL_NOT_FOUND("HOSPITAL-001", "存在しない病院です。", HttpStatus.NOT_FOUND),
    HOSPITAL_SCHEDULE_NOT_FOUND("HOSPITAL-002", "存在しない予約枠です。", HttpStatus.NOT_FOUND),
    HOSPITAL_BUSINESS_HOURS_NOT_FOUND("HOSPITAL-003", "存在しない営業時間です。", HttpStatus.NOT_FOUND),
    HOSPITAL_BUSINESS_HOURS_DUPLICATED("HOSPITAL-004", "既に登録されている曜日です。", HttpStatus.CONFLICT),
    DEFAULT("HOSPITAL-999", "病院関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

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
        return new HospitalException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new HospitalException(this, cause);
    }
}

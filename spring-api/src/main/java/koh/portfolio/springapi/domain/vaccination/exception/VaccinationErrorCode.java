package koh.portfolio.springapi.domain.vaccination.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum VaccinationErrorCode implements ErrorCode {
    VACCINATION_NOT_FOUND("VACCINATION-001", "存在しないワクチン接種予定です。", HttpStatus.NOT_FOUND),
    NOT_PET_OWNER("VACCINATION-002", "存在しないペット、または他のユーザーのペットです。", HttpStatus.FORBIDDEN),
    INVALID_STATE_TRANSITION("VACCINATION-003", "現在の状態では変更できません。", HttpStatus.CONFLICT),
    DEFAULT("VACCINATION-999", "ワクチン接種関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

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
        return new VaccinationException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new VaccinationException(this, cause);
    }
}

package koh.portfolio.springapi.domain.pet.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PetErrorCode implements ErrorCode {
    PET_NOT_FOUND("PET-001", "存在しないペットです。", HttpStatus.NOT_FOUND),
    NOT_PET_OWNER("PET-002", "飼い主が異なります。", HttpStatus.UNAUTHORIZED),
    DEFAULT("PET-999", "ペット関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

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
        return new PetException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new PetException(this, cause);
    }
}

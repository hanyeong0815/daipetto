package koh.portfolio.springapi.domain.pet.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class PetException extends CustomException {
    public PetException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PetException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

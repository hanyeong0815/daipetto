package koh.portfolio.springapi.domain.vaccination.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class VaccinationException extends CustomException {
    public VaccinationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VaccinationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

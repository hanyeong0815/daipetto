package koh.portfolio.springapi.domain.hospital.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class HospitalException extends CustomException {
    public HospitalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public HospitalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

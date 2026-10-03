package koh.portfolio.springapi.domain.healthrecord.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class HealthRecordException extends CustomException {
    public HealthRecordException(ErrorCode errorCode) {
        super(errorCode);
    }

    public HealthRecordException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

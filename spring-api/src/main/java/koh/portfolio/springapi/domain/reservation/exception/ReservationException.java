package koh.portfolio.springapi.domain.reservation.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class ReservationException extends CustomException {
    public ReservationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReservationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

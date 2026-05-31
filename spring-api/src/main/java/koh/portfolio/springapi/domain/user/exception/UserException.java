package koh.portfolio.springapi.domain.user.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class UserException extends CustomException {
    public UserException(ErrorCode errorCode) {

        super(errorCode);

    }

    public UserException(ErrorCode errorCode, Throwable cause) {

        super(errorCode, cause);

    }
}

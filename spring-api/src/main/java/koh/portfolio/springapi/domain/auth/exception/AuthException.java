package koh.portfolio.springapi.domain.auth.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class AuthException extends CustomException {
    public AuthException(ErrorCode errorCode) {

        super(errorCode);

    }

    public AuthException(ErrorCode errorCode, Throwable cause) {

        super(errorCode, cause);

    }
}

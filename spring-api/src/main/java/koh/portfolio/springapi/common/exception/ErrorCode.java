package koh.portfolio.springapi.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode { // public enum SomeErrorCode implements ErrorCode { … }
    String name();
    String code();
    HttpStatus defaultHttpStatus();
    String defaultMessage();
    RuntimeException defaultException();
    RuntimeException defaultException(Throwable cause);
}

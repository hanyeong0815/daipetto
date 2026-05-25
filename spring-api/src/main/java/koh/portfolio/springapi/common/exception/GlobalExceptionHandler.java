package koh.portfolio.springapi.common.exception;

import koh.portfolio.springapi.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        log.warn("CustomException occurred. code={}, message={}",
                errorCode.code(),
                errorCode.defaultMessage(),
                exception
        );

        return ResponseEntity
                .status(errorCode.defaultHttpStatus())
                .body(ApiResponse.error(errorCode.code(), errorCode.defaultMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();

        String message = fieldError == null
                ? "入力内容を確認してください。"
                : fieldError.getDefaultMessage();

        log.warn("Validation exception occurred. message={}", message);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("VALIDATION-001", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unexpected exception occurred.", exception);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("SERVER-001", "server error"));
    }
}
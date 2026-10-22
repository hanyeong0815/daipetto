package koh.portfolio.springapi.common.exception;

import koh.portfolio.springapi.common.response.ApiResponse;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // 型変換できないクエリパラメータ（例: role=FOO）、必須クエリパラメータの欠落、読み取れないJSON（不正なenum値など）は
    // 利用者の入力誤りのため、500ではなく400（VALIDATION-001）で返す
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnreadableInput(Exception exception) {
        log.warn("Unreadable request input. message={}", exception.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("VALIDATION-001", "入力内容を確認してください。"));
    }

    // 存在しないURL・許可されていないHTTPメソッド・対応していないContent-Typeも利用者の要求の誤りのため、
    // 下の汎用ハンドラーで500にせず本来のステータスで返す（単体テスト UT-13-004/005/007 で発見）
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("COMMON-001", "存在しないURLです。"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("COMMON-002", "許可されていないHTTPメソッドです。"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("COMMON-003", "対応していないContent-Typeです。"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException exception
    ) {
        AuthErrorCode errorCode = AuthErrorCode.ACCESS_DENIED;

        return ResponseEntity
                .status(errorCode.defaultHttpStatus())
                .body(ApiResponse.error(
                        errorCode.code(),
                        errorCode.defaultMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unexpected exception occurred.", exception);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("SERVER-001", "サーバーでエラーが発生しました。時間をおいて再度お試しください。"));
    }
}
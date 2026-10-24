package koh.portfolio.springapi.domain.analysis.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AnalysisErrorCode implements ErrorCode {
    NOT_PET_OWNER("ANALYSIS-001", "存在しないペット、または他のユーザーのペットです。", HttpStatus.FORBIDDEN),
    INVALID_PERIOD("ANALYSIS-002", "期間の指定が不正です。", HttpStatus.BAD_REQUEST),
    ANALYSIS_UNAVAILABLE("ANALYSIS-003", "分析サービスを利用できません。時間をおいて再度お試しください。", HttpStatus.SERVICE_UNAVAILABLE),
    DEFAULT("ANALYSIS-999", "分析関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public RuntimeException defaultException() {
        return new AnalysisException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new AnalysisException(this, cause);
    }
}

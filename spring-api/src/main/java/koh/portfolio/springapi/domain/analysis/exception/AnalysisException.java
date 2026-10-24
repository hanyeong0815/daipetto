package koh.portfolio.springapi.domain.analysis.exception;

import koh.portfolio.springapi.common.exception.CustomException;
import koh.portfolio.springapi.common.exception.ErrorCode;

public class AnalysisException extends CustomException {
    public AnalysisException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AnalysisException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

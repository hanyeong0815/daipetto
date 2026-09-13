package koh.portfolio.springapi.domain.reservation.exception;

import koh.portfolio.springapi.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {
    RESERVATION_DUPLICATED("RESERVATION-001", "既に予約済みです。", HttpStatus.CONFLICT),
    RESERVATION_SCHEDULE_NOT_FOUND("RESERVATION-002", "存在しない予約枠、または指定した病院に属さない予約枠です。", HttpStatus.NOT_FOUND),
    RESERVATION_SCHEDULE_BLOCKED("RESERVATION-003", "予約枠がBLOCKED状態のため予約できません。", HttpStatus.CONFLICT),
    NOT_PET_OWNER("RESERVATION-004", "存在しないペット、または他のユーザーのペットです。", HttpStatus.FORBIDDEN),
    PAST_DATETIME("RESERVATION-005", "過去の日時には予約できません。", HttpStatus.BAD_REQUEST),
    RESERVATION_NOT_FOUND("RESERVATION-006", "存在しない予約です。", HttpStatus.NOT_FOUND),
    NOT_RESERVATION_OWNER("RESERVATION-007", "予約者本人ではありません。", HttpStatus.FORBIDDEN),
    INVALID_STATE_TRANSITION("RESERVATION-008", "現在の状態からは変更できません。", HttpStatus.CONFLICT),
    DEFAULT("RESERVATION-999", "予約関連エラーです。", HttpStatus.INTERNAL_SERVER_ERROR);

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
        return new ReservationException(this);
    }

    @Override
    public RuntimeException defaultException(Throwable cause) {
        return new ReservationException(this, cause);
    }
}

package koh.portfolio.springapi.domain.reservation.model;

import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Reservation {
    private Long id;
    private Long userId;
    private Long petId;
    private Long hospitalId;
    private Long scheduleId;
    private LocalDateTime reservationDatetime;
    private ReservationStatus status;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Reservation create(Long userId, Long petId, Long hospitalId, Long scheduleId,
                                      LocalDateTime reservationDatetime, String memo) {
        LocalDateTime now = LocalDateTime.now();

        return new Reservation(null, userId, petId, hospitalId, scheduleId, reservationDatetime,
                ReservationStatus.REQUESTED, memo, now, now, null);
    }

    public Reservation approve() {
        Preconditions.validate(this.status == ReservationStatus.REQUESTED, ReservationErrorCode.INVALID_STATE_TRANSITION);

        return new Reservation(this.id, this.userId, this.petId, this.hospitalId, this.scheduleId,
                this.reservationDatetime, ReservationStatus.APPROVED, this.memo, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    public Reservation cancel() {
        Preconditions.validate(
                this.status == ReservationStatus.REQUESTED || this.status == ReservationStatus.APPROVED,
                ReservationErrorCode.INVALID_STATE_TRANSITION
        );

        return new Reservation(this.id, this.userId, this.petId, this.hospitalId, this.scheduleId,
                this.reservationDatetime, ReservationStatus.CANCELLED, this.memo, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }

    public Reservation complete() {
        Preconditions.validate(this.status == ReservationStatus.APPROVED, ReservationErrorCode.INVALID_STATE_TRANSITION);

        return new Reservation(this.id, this.userId, this.petId, this.hospitalId, this.scheduleId,
                this.reservationDatetime, ReservationStatus.COMPLETED, this.memo, this.createdAt, LocalDateTime.now(), this.deletedAt);
    }
}

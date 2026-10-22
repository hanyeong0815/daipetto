package koh.portfolio.springapi.application.reservation.usecase;

public interface RejectReservationUseCase {
    void execute(Long userId, Long reservationId);
}

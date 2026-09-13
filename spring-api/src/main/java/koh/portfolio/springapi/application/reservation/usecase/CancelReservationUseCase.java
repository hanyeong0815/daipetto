package koh.portfolio.springapi.application.reservation.usecase;

public interface CancelReservationUseCase {
    void execute(Long userId, Long reservationId);
}

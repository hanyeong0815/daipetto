package koh.portfolio.springapi.application.reservation.usecase;

public interface CompleteReservationUseCase {
    void execute(Long userId, Long reservationId);
}

package koh.portfolio.springapi.application.reservation.usecase;

public interface ApproveReservationUseCase {
    void execute(Long userId, Long reservationId);
}

package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.notification.usecase.NotifyReservationEventUseCase;
import koh.portfolio.springapi.application.reservation.usecase.RejectReservationUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.notification.model.NotificationType;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectReservationService implements RejectReservationUseCase {
    private final ReservationRepository reservationRepository;
    private final NotifyReservationEventUseCase notifyReservationEventUseCase;

    @Override
    @Transactional
    public void execute(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrorCode.RESERVATION_NOT_FOUND::defaultException);

        Reservation rejected = reservation.reject();

        Preconditions.validate(
                reservationRepository.updateStatus(
                        reservation.getId(), reservation.getStatus(), rejected.getStatus(), rejected.getUpdatedAt()),
                ReservationErrorCode.INVALID_STATE_TRANSITION
        );

        notifyReservationEventUseCase.execute(
                reservation.getUserId(), reservation.getId(), NotificationType.RESERVATION_REJECTED);
    }
}

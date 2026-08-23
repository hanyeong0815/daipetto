package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.reservation.usecase.CancelReservationUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelReservationService implements CancelReservationUseCase {
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public void execute(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrorCode.RESERVATION_NOT_FOUND::defaultException);

        Preconditions.validate(reservation.getUserId().equals(userId), ReservationErrorCode.NOT_RESERVATION_OWNER);

        reservationRepository.save(reservation.cancel());
    }
}

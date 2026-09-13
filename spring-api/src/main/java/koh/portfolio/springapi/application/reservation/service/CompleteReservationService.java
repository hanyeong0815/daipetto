package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.reservation.usecase.CompleteReservationUseCase;
import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteReservationService implements CompleteReservationUseCase {
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public void execute(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrorCode.RESERVATION_NOT_FOUND::defaultException);

        reservationRepository.save(reservation.complete());
    }
}

package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.notification.usecase.NotifyReservationEventUseCase;
import koh.portfolio.springapi.application.hospital.service.HospitalAccessChecker;
import koh.portfolio.springapi.application.reservation.usecase.CompleteReservationUseCase;
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
public class CompleteReservationService implements CompleteReservationUseCase {
    private final HospitalAccessChecker hospitalAccessChecker;
    private final ReservationRepository reservationRepository;
    private final NotifyReservationEventUseCase notifyReservationEventUseCase;

    @Override
    @Transactional
    public void execute(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrorCode.RESERVATION_NOT_FOUND::defaultException);

        // 病院管理者は紐付いた病院の予約だけを操作できる（06_ERD §11）
        hospitalAccessChecker.checkManageable(userId, reservation.getHospitalId());

        Reservation completed = reservation.complete();

        Preconditions.validate(
                reservationRepository.updateStatus(
                        reservation.getId(), reservation.getStatus(), completed.getStatus(), completed.getUpdatedAt()),
                ReservationErrorCode.INVALID_STATE_TRANSITION
        );

        notifyReservationEventUseCase.execute(
                reservation.getUserId(), reservation.getId(), NotificationType.TREATMENT_COMPLETED);
    }
}

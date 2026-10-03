package koh.portfolio.springapi.application.reservation.service;

import koh.portfolio.springapi.application.notification.usecase.NotifyReservationEventUseCase;
import koh.portfolio.springapi.application.reservation.usecase.ApproveReservationUseCase;
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
public class ApproveReservationService implements ApproveReservationUseCase {
    private final ReservationRepository reservationRepository;
    private final NotifyReservationEventUseCase notifyReservationEventUseCase;

    @Override
    @Transactional
    public void execute(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(ReservationErrorCode.RESERVATION_NOT_FOUND::defaultException);

        Reservation approved = reservation.approve();

        // 読み取り時点の状態を条件に含めることで、却下・キャンセルと同時実行しても
        // 先に成立した終了状態を上書きしない（負けた側はRESERVATION-008）
        Preconditions.validate(
                reservationRepository.updateStatus(
                        reservation.getId(), reservation.getStatus(), approved.getStatus(), approved.getUpdatedAt()),
                ReservationErrorCode.INVALID_STATE_TRANSITION
        );

        // 遷移が成立した場合のみ通知を生成する（08_State_Design §6-6）
        notifyReservationEventUseCase.execute(
                reservation.getUserId(), reservation.getId(), NotificationType.RESERVATION_APPROVED);
    }
}

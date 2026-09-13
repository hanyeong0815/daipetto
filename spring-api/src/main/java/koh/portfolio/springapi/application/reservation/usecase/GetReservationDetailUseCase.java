package koh.portfolio.springapi.application.reservation.usecase;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationDetailResponse;

public interface GetReservationDetailUseCase {
    ReservationDetailResponse execute(Long userId, Long reservationId);
}

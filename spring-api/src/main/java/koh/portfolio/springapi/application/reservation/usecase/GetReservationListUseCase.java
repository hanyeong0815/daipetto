package koh.portfolio.springapi.application.reservation.usecase;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.ReservationSummary;

import java.util.List;

public interface GetReservationListUseCase {
    List<ReservationSummary> execute(Long userId);
}

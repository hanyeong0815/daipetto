package koh.portfolio.springapi.application.reservation.usecase;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.AdminReservationResponse;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

public interface GetAdminReservationListUseCase {
    List<AdminReservationResponse> execute(Long userId, Long hospitalId, ReservationStatus status, LocalDate date);
}

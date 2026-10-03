package koh.portfolio.springapi.application.reservation.usecase;

import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationRequest;
import koh.portfolio.springapi.application.reservation.dto.ReservationDto.CreateReservationResponse;

public interface CreateReservationUseCase {
    CreateReservationResponse execute(Long userId, CreateReservationRequest request);
}

package koh.portfolio.springapi.domain.reservation.port;

import koh.portfolio.springapi.domain.reservation.model.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findAllByUserId(Long userId);
    boolean existsActiveByScheduleId(Long scheduleId);
}

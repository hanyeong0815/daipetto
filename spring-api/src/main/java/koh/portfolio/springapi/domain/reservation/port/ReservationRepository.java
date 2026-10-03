package koh.portfolio.springapi.domain.reservation.port;

import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findAllByUserId(Long userId);
    boolean existsActiveByScheduleId(Long scheduleId);

    // expectedStatus と一致する行だけを更新し、更新できたかを返す。
    // 同時に別の遷移が成立していた場合は false となり、終了状態の上書きを防ぐ
    boolean updateStatus(Long reservationId, ReservationStatus expectedStatus, ReservationStatus newStatus, LocalDateTime updatedAt);
}

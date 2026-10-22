package koh.portfolio.springapi.domain.reservation.port;

import koh.portfolio.springapi.domain.reservation.model.AdminReservationView;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    List<Reservation> findAllByUserId(Long userId);
    boolean existsActiveByScheduleId(Long scheduleId);

    // 病院の予約一覧（論理削除済みを除く）。status・date が null の条件では絞り込まない。予約日・開始時刻・ID の昇順
    List<AdminReservationView> findAllForAdmin(Long hospitalId, ReservationStatus status, LocalDate date);

    // expectedStatus と一致する行だけを更新し、更新できたかを返す。
    // 同時に別の遷移が成立していた場合は false となり、終了状態の上書きを防ぐ
    boolean updateStatus(Long reservationId, ReservationStatus expectedStatus, ReservationStatus newStatus, LocalDateTime updatedAt);
}

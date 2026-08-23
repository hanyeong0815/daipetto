package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    Optional<ReservationEntity> findByIdAndDeletedAtIsNull(Long id);

    List<ReservationEntity> findAllByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByScheduleIdAndStatusInAndDeletedAtIsNull(Long scheduleId, List<ReservationStatus> statuses);
}

package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.domain.reservation.exception.ReservationErrorCode;
import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationPersistenceAdapter implements ReservationRepository {
    private static final String ACTIVE_SCHEDULE_CONSTRAINT = "uq_reservations_active_schedule";

    private final ReservationJpaRepository reservationJpaRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity reservationEntity = reservationMapper.toEntity(reservation);

        try {
            // uq_reservations_active_schedule（V8）違反をここで検知するためflushまで行う。
            // commit時まで遅延させるとSERVER-001として外に出てしまう
            ReservationEntity savedEntity = reservationJpaRepository.saveAndFlush(reservationEntity);

            return reservationMapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException e) {
            // 予約枠の一意制約だけをRESERVATION-001に変換する。
            // 他の整合性違反まで「重複予約」に見せると原因を隠してしまう
            if (!isActiveScheduleViolation(e)) {
                throw e;
            }

            throw ReservationErrorCode.RESERVATION_DUPLICATED.defaultException(e);
        }
    }

    private boolean isActiveScheduleViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException violation
                && ACTIVE_SCHEDULE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
            return true;
        }

        String message = e.getMostSpecificCause().getMessage();

        return message != null && message.toLowerCase().contains(ACTIVE_SCHEDULE_CONSTRAINT);
    }

    @Override
    public boolean updateStatus(Long reservationId, ReservationStatus expectedStatus, ReservationStatus newStatus, LocalDateTime updatedAt) {
        return reservationJpaRepository.updateStatus(reservationId, expectedStatus, newStatus, updatedAt) == 1;
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationJpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(reservationMapper::toDomain);
    }

    @Override
    public List<Reservation> findAllByUserId(Long userId) {
        return reservationJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .stream().map(reservationMapper::toDomain).toList();
    }

    @Override
    public boolean existsActiveByScheduleId(Long scheduleId) {
        return reservationJpaRepository.existsByScheduleIdAndStatusInAndDeletedAtIsNull(
                scheduleId, List.of(ReservationStatus.REQUESTED, ReservationStatus.APPROVED)
        );
    }
}

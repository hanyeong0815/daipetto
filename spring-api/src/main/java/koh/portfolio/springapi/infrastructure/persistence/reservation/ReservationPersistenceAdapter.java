package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.domain.reservation.model.Reservation;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import koh.portfolio.springapi.domain.reservation.port.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationPersistenceAdapter implements ReservationRepository {
    private final ReservationJpaRepository reservationJpaRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity reservationEntity = reservationMapper.toEntity(reservation);

        ReservationEntity savedEntity = reservationJpaRepository.save(reservationEntity);

        return reservationMapper.toDomain(savedEntity);
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

package koh.portfolio.springapi.infrastructure.persistence.reservation;

import koh.portfolio.springapi.domain.reservation.model.AdminReservationView;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    Optional<ReservationEntity> findByIdAndDeletedAtIsNull(Long id);

    List<ReservationEntity> findAllByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByScheduleIdAndStatusInAndDeletedAtIsNull(Long scheduleId, List<ReservationStatus> statuses);
    // null の条件は CAST で型を明示する（12_Trouble_Shooting §3-10）。ペットは論理削除後も予約履歴に名前を出すため結合する
    @Query("select new koh.portfolio.springapi.domain.reservation.model.AdminReservationView("
            + "r.id, r.status, s.availableDate, s.startTime, s.endTime, u.id, u.nickname, p.name, p.petType, r.memo, r.createdAt) "
            + "from ReservationEntity r "
            + "join HospitalScheduleEntity s on s.id = r.scheduleId "
            + "join UserEntity u on u.id = r.userId "
            + "join PetEntity p on p.id = r.petId "
            + "where r.hospitalId = :hospitalId and r.deletedAt is null "
            + "and (:status is null or r.status = :status) "
            + "and (cast(:date as LocalDate) is null or s.availableDate = :date) "
            + "order by s.availableDate, s.startTime, r.id")
    List<AdminReservationView> findAllForAdmin(
            @Param("hospitalId") Long hospitalId,
            @Param("status") ReservationStatus status,
            @Param("date") LocalDate date
    );


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ReservationEntity set status = :newStatus, updatedAt = :updatedAt "
            + "where id = :id and status = :expectedStatus and deletedAt is null")
    int updateStatus(
            @Param("id") Long id,
            @Param("expectedStatus") ReservationStatus expectedStatus,
            @Param("newStatus") ReservationStatus newStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

package koh.portfolio.springapi.infrastructure.persistence.vaccination;

import koh.portfolio.springapi.domain.vaccination.model.VaccinationReminderTarget;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VaccinationJpaRepository extends JpaRepository<VaccinationEntity, Long> {
    Optional<VaccinationEntity> findByIdAndDeletedAtIsNull(Long id);

    List<VaccinationEntity> findAllByPetIdAndDeletedAtIsNullOrderByScheduledDateAsc(Long petId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VaccinationEntity set status = :newStatus, completedDate = :completedDate, updatedAt = :updatedAt "
            + "where id = :id and status = :expectedStatus and deletedAt is null")
    int markCompleted(
            @Param("id") Long id,
            @Param("expectedStatus") VaccinationStatus expectedStatus,
            @Param("newStatus") VaccinationStatus newStatus,
            @Param("completedDate") LocalDate completedDate,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VaccinationEntity set status = :newStatus, scheduledDate = :scheduledDate, completedDate = null, "
            + "remindedDate = null, updatedAt = :updatedAt where id = :id and status = :expectedStatus and deletedAt is null")
    int reschedule(
            @Param("id") Long id,
            @Param("expectedStatus") VaccinationStatus expectedStatus,
            @Param("newStatus") VaccinationStatus newStatus,
            @Param("scheduledDate") LocalDate scheduledDate,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VaccinationEntity set status = :newStatus, updatedAt = :updatedAt "
            + "where id = :id and status = :expectedStatus and deletedAt is null")
    int cancel(
            @Param("id") Long id,
            @Param("expectedStatus") VaccinationStatus expectedStatus,
            @Param("newStatus") VaccinationStatus newStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    // 抽出時の予定日のまま予定中で未送信の場合だけ目印を書く。抽出後に中止・再登録・完了・削除が確定していれば0件
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VaccinationEntity set remindedDate = :scheduledDate where id = :id and status = :status "
            + "and deletedAt is null and scheduledDate = :scheduledDate and remindedDate is null")
    int markReminded(
            @Param("id") Long id,
            @Param("status") VaccinationStatus status,
            @Param("scheduledDate") LocalDate scheduledDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VaccinationEntity set deletedAt = :deletedAt where id = :id and deletedAt is null")
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    // 送信済みの目印が無いものだけを抽出する。再登録で目印が消えるため、新しい予定日で再び抽出される
    @Query("select new koh.portfolio.springapi.domain.vaccination.model.VaccinationReminderTarget("
            + "v.id, p.userId, p.name, v.vaccineName, v.scheduledDate) "
            + "from VaccinationEntity v join PetEntity p on p.id = v.petId "
            + "where v.status = :status and v.deletedAt is null and p.deletedAt is null "
            + "and v.scheduledDate between :from and :to and v.remindedDate is null "
            + "order by v.scheduledDate, v.id")
    List<VaccinationReminderTarget> findReminderTargets(
            @Param("status") VaccinationStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}

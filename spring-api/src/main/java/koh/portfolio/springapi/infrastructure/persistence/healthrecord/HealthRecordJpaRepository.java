package koh.portfolio.springapi.infrastructure.persistence.healthrecord;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthRecordJpaRepository extends JpaRepository<HealthRecordEntity, Long> {
    Optional<HealthRecordEntity> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HealthRecordEntity h where h.id = :id and h.deletedAt is null")
    Optional<HealthRecordEntity> findByIdForUpdate(@Param("id") Long id);

    List<HealthRecordEntity> findAllByPetIdAndDeletedAtIsNullOrderByRecordedDateDesc(Long petId);

    @Modifying
    @Query("UPDATE HealthRecordEntity SET deletedAt = ?2 WHERE id = ?1")
    void softDeleteById(Long id, LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update HealthRecordEntity set weight = :weight, symptom = :symptom, memo = :memo, "
            + "recordedDate = :recordedDate, updatedAt = :updatedAt where id = :id and deletedAt is null")
    int updateContent(
            @Param("id") Long id,
            @Param("weight") BigDecimal weight,
            @Param("symptom") String symptom,
            @Param("memo") String memo,
            @Param("recordedDate") LocalDate recordedDate,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

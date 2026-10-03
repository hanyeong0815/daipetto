package koh.portfolio.springapi.infrastructure.persistence.healthrecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthRecordJpaRepository extends JpaRepository<HealthRecordEntity, Long> {
    Optional<HealthRecordEntity> findByIdAndDeletedAtIsNull(Long id);

    List<HealthRecordEntity> findAllByPetIdAndDeletedAtIsNullOrderByRecordedDateDesc(Long petId);

    @Modifying
    @Query("UPDATE HealthRecordEntity SET deletedAt = ?2 WHERE id = ?1")
    void softDeleteById(Long id, LocalDateTime deletedAt);
}

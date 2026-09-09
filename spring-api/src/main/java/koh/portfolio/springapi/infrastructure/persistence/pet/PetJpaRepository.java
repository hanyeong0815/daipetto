package koh.portfolio.springapi.infrastructure.persistence.pet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PetJpaRepository extends JpaRepository<PetEntity, Long> {
    boolean existsByUserIdAndName(Long userId, String name);

    Optional<PetEntity> findByIdAndDeletedAtIsNull(Long id);

    List<PetEntity> findAllByUserIdAndDeletedAtIsNull(Long userId);

    Optional<PetEntity> findByUserIdAndName(Long userId, String name);

    @Modifying
    @Query("UPDATE PetEntity SET deletedAt = ?2 WHERE id = ?1")
    void softDeleteById(Long id, LocalDateTime deletedAt);
}

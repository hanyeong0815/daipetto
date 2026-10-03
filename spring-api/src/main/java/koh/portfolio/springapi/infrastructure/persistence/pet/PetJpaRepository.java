package koh.portfolio.springapi.infrastructure.persistence.pet;

import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PetEntity set name = :name, petType = :petType, birthDate = :birthDate, gender = :gender, "
            + "weight = :weight, updatedAt = :updatedAt where id = :id and deletedAt is null")
    int updateProfile(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("petType") PetType petType,
            @Param("birthDate") LocalDate birthDate,
            @Param("gender") PetGender gender,
            @Param("weight") BigDecimal weight,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

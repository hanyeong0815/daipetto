package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HospitalJpaRepository extends JpaRepository<HospitalEntity, Long> {
    Optional<HospitalEntity> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            SELECT h FROM HospitalEntity h
            WHERE h.deletedAt IS NULL
            AND h.status = :status
            AND (:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            AND (:area IS NULL OR LOWER(h.address) LIKE LOWER(CONCAT('%', CAST(:area AS string), '%')))
            """)
    List<HospitalEntity> search(
            @Param("status") HospitalStatus status,
            @Param("keyword") String keyword,
            @Param("area") String area
    );
}

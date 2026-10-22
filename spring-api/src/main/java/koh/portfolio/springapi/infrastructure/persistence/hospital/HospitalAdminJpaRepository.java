package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalAdminSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HospitalAdminJpaRepository extends JpaRepository<HospitalAdminEntity, Long> {
    boolean existsByUserIdAndHospitalId(Long userId, Long hospitalId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from HospitalAdminEntity a where a.userId = :userId and a.hospitalId = :hospitalId")
    int deleteLink(@Param("userId") Long userId, @Param("hospitalId") Long hospitalId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from HospitalAdminEntity a where a.userId = :userId")
    int deleteAllByUser(@Param("userId") Long userId);

    @Query("select a.hospitalId from HospitalAdminEntity a where a.userId = :userId order by a.hospitalId")
    List<Long> findHospitalIdsByUserId(@Param("userId") Long userId);

    @Query("select new koh.portfolio.springapi.domain.hospital.model.HospitalAdminSummary(u.id, u.email, u.nickname) "
            + "from HospitalAdminEntity a join UserEntity u on u.id = a.userId "
            + "where a.hospitalId = :hospitalId and u.deletedAt is null order by u.id")
    List<HospitalAdminSummary> findAdminsByHospitalId(@Param("hospitalId") Long hospitalId);
}

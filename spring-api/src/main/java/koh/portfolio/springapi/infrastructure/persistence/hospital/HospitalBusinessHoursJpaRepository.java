package koh.portfolio.springapi.infrastructure.persistence.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface HospitalBusinessHoursJpaRepository extends JpaRepository<HospitalBusinessHoursEntity, Long> {
    List<HospitalBusinessHoursEntity> findByHospitalId(Long hospitalId);
    boolean existsByHospitalIdAndDayOfWeek(Long hospitalId, DayOfWeek dayOfWeek);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update HospitalBusinessHoursEntity set openTime = :openTime, closeTime = :closeTime, "
            + "breakStartTime = :breakStartTime, breakEndTime = :breakEndTime, "
            + "slotDurationMinutes = :slotDurationMinutes, updatedAt = :updatedAt "
            + "where id = :id and hospitalId = :hospitalId")
    int updateHours(
            @Param("id") Long id,
            @Param("hospitalId") Long hospitalId,
            @Param("openTime") LocalTime openTime,
            @Param("closeTime") LocalTime closeTime,
            @Param("breakStartTime") LocalTime breakStartTime,
            @Param("breakEndTime") LocalTime breakEndTime,
            @Param("slotDurationMinutes") Integer slotDurationMinutes,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

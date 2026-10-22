package koh.portfolio.springapi.infrastructure.persistence.hospital;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HospitalScheduleJpaRepository extends JpaRepository<HospitalScheduleEntity, Long> {
    List<HospitalScheduleEntity> findAllByHospitalIdOrderByAvailableDateAscStartTimeAsc(Long hospitalId);

    @Query("select distinct s.availableDate from HospitalScheduleEntity s "
            + "where s.hospitalId = :hospitalId and s.availableDate between :from and :to")
    List<LocalDate> findDatesWithSchedules(
            @Param("hospitalId") Long hospitalId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}

package koh.portfolio.springapi.infrastructure.persistence.hospital;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface HospitalBusinessHoursJpaRepository extends JpaRepository<HospitalBusinessHoursEntity, Long> {
    List<HospitalBusinessHoursEntity> findByHospitalId(Long hospitalId);
    boolean existsByHospitalIdAndDayOfWeek(Long hospitalId, DayOfWeek dayOfWeek);
}

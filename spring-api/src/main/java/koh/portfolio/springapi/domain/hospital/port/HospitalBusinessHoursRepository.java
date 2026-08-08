package koh.portfolio.springapi.domain.hospital.port;

import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface HospitalBusinessHoursRepository {
    HospitalBusinessHours save(HospitalBusinessHours hospitalBusinessHours);
    Optional<HospitalBusinessHours> findById(Long id);
    List<HospitalBusinessHours> findByHospitalId(Long hospitalId);
    boolean existsByHospitalIdAndDayOfWeek(Long hospitalId, DayOfWeek dayOfWeek);
    void deleteById(Long id);
}

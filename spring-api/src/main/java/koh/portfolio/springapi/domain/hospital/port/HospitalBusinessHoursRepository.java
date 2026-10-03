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

    // 時間列だけを更新し、更新できたかを返す。エンティティ全体のmergeだと、
    // 読み取り後に削除された行を新しい行として作り直してしまう（物理削除のため）
    boolean updateHours(HospitalBusinessHours hospitalBusinessHours);
}

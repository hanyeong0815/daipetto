package koh.portfolio.springapi.domain.hospital.port;

import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HospitalScheduleRepository {
    List<HospitalSchedule> findAllByHospitalId(Long hospitalId);
    HospitalSchedule save(HospitalSchedule hospitalSchedule);
    Optional<HospitalSchedule> findById(Long id);

    // 予約枠の自動生成用。期間内で既に予約枠がある日（手動の枠・BLOCKEDを含む）と、一括保存
    List<LocalDate> findDatesWithSchedules(Long hospitalId, LocalDate from, LocalDate to);
    void saveAll(List<HospitalSchedule> hospitalSchedules);
}

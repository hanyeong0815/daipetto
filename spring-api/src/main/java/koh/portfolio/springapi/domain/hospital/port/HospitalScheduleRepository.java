package koh.portfolio.springapi.domain.hospital.port;

import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;

import java.util.List;
import java.util.Optional;

public interface HospitalScheduleRepository {
    List<HospitalSchedule> findAllByHospitalId(Long hospitalId);
    HospitalSchedule save(HospitalSchedule hospitalSchedule);
    Optional<HospitalSchedule> findById(Long id);
}

package koh.portfolio.springapi.domain.hospital.port;

import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;

import java.util.List;

public interface HospitalScheduleRepository {
    List<HospitalSchedule> findAllByHospitalId(Long hospitalId);
}

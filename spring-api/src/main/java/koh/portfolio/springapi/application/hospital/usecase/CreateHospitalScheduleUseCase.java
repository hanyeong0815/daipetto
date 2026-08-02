package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleResponse;

public interface CreateHospitalScheduleUseCase {
    CreateHospitalScheduleResponse execute(Long hospitalId, CreateHospitalScheduleRequest request);
}

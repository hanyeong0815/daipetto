package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursResponse;

public interface CreateHospitalBusinessHoursUseCase {
    CreateHospitalBusinessHoursResponse execute(Long hospitalId, CreateHospitalBusinessHoursRequest request);
}

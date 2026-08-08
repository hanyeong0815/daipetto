package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.UpdateHospitalBusinessHoursRequest;

public interface UpdateHospitalBusinessHoursUseCase {
    void execute(Long hospitalId, Long businessHoursId, UpdateHospitalBusinessHoursRequest request);
}

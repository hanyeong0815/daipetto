package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.UpdateHospitalRequest;

public interface UpdateHospitalUseCase {
    void execute(Long hospitalId, UpdateHospitalRequest request);
}

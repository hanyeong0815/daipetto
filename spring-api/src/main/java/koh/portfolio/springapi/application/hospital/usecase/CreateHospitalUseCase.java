package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalResponse;

public interface CreateHospitalUseCase {
    CreateHospitalResponse execute(CreateHospitalRequest request);
}

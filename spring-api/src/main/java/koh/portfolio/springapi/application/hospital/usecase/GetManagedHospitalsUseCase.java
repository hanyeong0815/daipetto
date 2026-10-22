package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.ManagedHospitalResponse;

import java.util.List;

public interface GetManagedHospitalsUseCase {
    List<ManagedHospitalResponse> execute(Long userId);
}

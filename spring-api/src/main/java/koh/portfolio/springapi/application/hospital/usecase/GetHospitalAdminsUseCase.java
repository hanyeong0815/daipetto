package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.HospitalAdminResponse;

import java.util.List;

public interface GetHospitalAdminsUseCase {
    List<HospitalAdminResponse> execute(Long hospitalId);
}

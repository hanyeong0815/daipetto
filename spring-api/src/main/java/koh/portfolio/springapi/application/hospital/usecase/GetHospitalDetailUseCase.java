package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalDetailResponse;

public interface GetHospitalDetailUseCase {
    HospitalDetailResponse execute(Long hospitalId);
}

package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.HospitalBusinessHoursResponse;

import java.util.List;

public interface GetHospitalBusinessHoursListUseCase {
    List<HospitalBusinessHoursResponse> execute(Long hospitalId);
}

package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.HospitalScheduleResponse;

import java.util.List;

public interface GetHospitalScheduleListUseCase {
    List<HospitalScheduleResponse> execute(Long hospitalId);
}

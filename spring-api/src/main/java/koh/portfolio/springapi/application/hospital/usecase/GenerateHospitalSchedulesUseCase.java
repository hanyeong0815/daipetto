package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.GenerateHospitalSchedulesResponse;

import java.time.YearMonth;

public interface GenerateHospitalSchedulesUseCase {
    GenerateHospitalSchedulesResponse execute(Long userId, Long hospitalId, YearMonth month);
}

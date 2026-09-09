package koh.portfolio.springapi.application.hospital.usecase;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalSummary;

import java.util.List;

public interface GetHospitalListUseCase {
    List<HospitalSummary> execute(String keyword, String area);
}

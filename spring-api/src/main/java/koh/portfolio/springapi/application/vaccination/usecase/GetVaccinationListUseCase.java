package koh.portfolio.springapi.application.vaccination.usecase;

import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.VaccinationResponse;

import java.util.List;

public interface GetVaccinationListUseCase {
    List<VaccinationResponse> execute(Long userId, Long petId);
}

package koh.portfolio.springapi.application.vaccination.usecase;

import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationRequest;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationResponse;

public interface CreateVaccinationUseCase {
    CreateVaccinationResponse execute(Long userId, Long petId, CreateVaccinationRequest request);
}

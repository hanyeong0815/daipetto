package koh.portfolio.springapi.application.vaccination.service;

import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationRequest;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.CreateVaccinationResponse;
import koh.portfolio.springapi.application.vaccination.dto.VaccinationDto.VaccinationResponse;
import koh.portfolio.springapi.application.vaccination.usecase.CreateVaccinationUseCase;
import koh.portfolio.springapi.application.vaccination.usecase.GetVaccinationListUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.vaccination.exception.VaccinationErrorCode;
import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VaccinationService implements CreateVaccinationUseCase, GetVaccinationListUseCase {
    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public CreateVaccinationResponse execute(Long userId, Long petId, CreateVaccinationRequest request) {
        validatePetOwner(userId, petId);

        Vaccination savedVaccination = vaccinationRepository.save(
                Vaccination.create(petId, request.vaccineName(), request.scheduledDate()));

        return CreateVaccinationResponse.builder()
                .vaccinationId(savedVaccination.getId())
                .build();
    }

    @Override
    public List<VaccinationResponse> execute(Long userId, Long petId) {
        validatePetOwner(userId, petId);

        return vaccinationRepository.findAllByPetId(petId).stream()
                .map(vaccination -> VaccinationResponse.builder()
                        .vaccinationId(vaccination.getId())
                        .vaccineName(vaccination.getVaccineName())
                        .scheduledDate(vaccination.getScheduledDate())
                        .completedDate(vaccination.getCompletedDate())
                        .status(vaccination.getStatus())
                        .build())
                .toList();
    }

    // ワクチン接種予定はペット経由で所有者を判定する（予定自体はuserIdを持たない）
    private void validatePetOwner(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId).orElse(null);

        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                VaccinationErrorCode.NOT_PET_OWNER
        );
    }
}

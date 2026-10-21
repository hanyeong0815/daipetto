package koh.portfolio.springapi.application.vaccination.service;

import koh.portfolio.springapi.application.vaccination.usecase.DeleteVaccinationUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.vaccination.exception.VaccinationErrorCode;
import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteVaccinationService implements DeleteVaccinationUseCase {
    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public void execute(Long userId, Long vaccinationId) {
        Vaccination vaccination = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(VaccinationErrorCode.VACCINATION_NOT_FOUND::defaultException);

        Pet pet = petRepository.findById(vaccination.getPetId()).orElse(null);
        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                VaccinationErrorCode.NOT_PET_OWNER
        );

        // 読み取り後に別リクエストが削除していた場合は0件となり、存在しない予定として扱う
        Preconditions.validate(
                vaccinationRepository.softDelete(vaccinationId, LocalDateTime.now()),
                VaccinationErrorCode.VACCINATION_NOT_FOUND
        );
    }
}

package koh.portfolio.springapi.application.vaccination.service;

import koh.portfolio.springapi.application.vaccination.usecase.CancelVaccinationUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.vaccination.exception.VaccinationErrorCode;
import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelVaccinationService implements CancelVaccinationUseCase {
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

        Vaccination cancelled = vaccination.cancel();

        // updated_at も進めるため、後で再登録した予定は改めてリマインダー対象になる
        Preconditions.validate(
                vaccinationRepository.cancel(cancelled.getId(), cancelled.getUpdatedAt()),
                VaccinationErrorCode.INVALID_STATE_TRANSITION
        );
    }
}

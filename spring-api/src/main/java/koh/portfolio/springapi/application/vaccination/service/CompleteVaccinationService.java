package koh.portfolio.springapi.application.vaccination.service;

import koh.portfolio.springapi.application.vaccination.usecase.CompleteVaccinationUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import koh.portfolio.springapi.domain.vaccination.exception.VaccinationErrorCode;
import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CompleteVaccinationService implements CompleteVaccinationUseCase {
    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public void execute(Long userId, Long vaccinationId, LocalDate completedDate) {
        Vaccination vaccination = vaccinationRepository.findById(vaccinationId)
                .orElseThrow(VaccinationErrorCode.VACCINATION_NOT_FOUND::defaultException);

        Pet pet = petRepository.findById(vaccination.getPetId()).orElse(null);
        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                VaccinationErrorCode.NOT_PET_OWNER
        );

        Vaccination completed = vaccination.complete(completedDate != null ? completedDate : LocalDate.now());

        // 読み取り後に別の遷移が確定していた場合は0件となり、その遷移を上書きしない
        Preconditions.validate(
                vaccinationRepository.markCompleted(
                        completed.getId(), completed.getCompletedDate(), completed.getUpdatedAt()),
                VaccinationErrorCode.INVALID_STATE_TRANSITION
        );
    }
}

package koh.portfolio.springapi.application.pet.service;

import koh.portfolio.springapi.application.pet.usecase.DeletePetUseCase;
import koh.portfolio.springapi.domain.pet.exception.PetErrorCode;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeletePetService implements DeletePetUseCase {
    private final PetRepository petRepository;

    @Override
    @Transactional
    public void execute(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(PetErrorCode.PET_NOT_FOUND::defaultException);

        if (!pet.getUserId().equals(userId)) {
            throw PetErrorCode.NOT_PET_OWNER.defaultException();
        }

        petRepository.softDelete(petId, LocalDateTime.now());
    }
}

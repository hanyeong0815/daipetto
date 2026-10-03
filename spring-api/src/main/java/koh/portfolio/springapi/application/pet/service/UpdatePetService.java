package koh.portfolio.springapi.application.pet.service;

import koh.portfolio.springapi.application.pet.dto.PetDto.UpdatePetRequest;
import koh.portfolio.springapi.application.pet.usecase.UpdatePetUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.pet.exception.PetErrorCode;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePetService implements UpdatePetUseCase {
    private final PetRepository petRepository;

    @Override
    @Transactional
    public void execute(Long userId, Long petId, UpdatePetRequest request) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(PetErrorCode.PET_NOT_FOUND::defaultException);

        Preconditions.validate(pet.getUserId().equals(userId), PetErrorCode.NOT_PET_OWNER);

        Pet updatedPet = pet.update(
                request.name(),
                request.petType(),
                request.birthDate(),
                request.gender(),
                request.weight()
        );

        // 読み取り後に論理削除がcommitされていた場合は0件更新。削除を最終状態として維持する
        Preconditions.validate(petRepository.updateProfile(updatedPet), PetErrorCode.PET_NOT_FOUND);
    }
}

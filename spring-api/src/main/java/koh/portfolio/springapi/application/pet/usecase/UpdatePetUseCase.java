package koh.portfolio.springapi.application.pet.usecase;

import koh.portfolio.springapi.application.pet.dto.PetDto.UpdatePetRequest;

public interface UpdatePetUseCase {
    void execute(Long userId, Long petId, UpdatePetRequest request);
}

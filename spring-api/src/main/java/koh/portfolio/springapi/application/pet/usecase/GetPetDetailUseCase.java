package koh.portfolio.springapi.application.pet.usecase;

import koh.portfolio.springapi.application.pet.dto.PetDto.PetDetailResponse;

public interface GetPetDetailUseCase {
    PetDetailResponse execute(Long userId, Long petId);
}

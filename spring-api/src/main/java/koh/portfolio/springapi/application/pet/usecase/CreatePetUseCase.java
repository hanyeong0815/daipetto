package koh.portfolio.springapi.application.pet.usecase;


import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetRequest;
import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetResponse;

public interface CreatePetUseCase {
    CreatePetResponse execute(Long userId, CreatePetRequest request);
}

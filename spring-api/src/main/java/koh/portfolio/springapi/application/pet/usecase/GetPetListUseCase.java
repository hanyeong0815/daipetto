package koh.portfolio.springapi.application.pet.usecase;

import koh.portfolio.springapi.application.pet.dto.PetDto.PetSummary;

import java.util.List;

public interface GetPetListUseCase {
    List<PetSummary> execute(Long userId);
}

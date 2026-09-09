package koh.portfolio.springapi.application.pet.service;

import koh.portfolio.springapi.application.pet.dto.PetDto.PetDetailResponse;
import koh.portfolio.springapi.application.pet.usecase.GetPetDetailUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.pet.exception.PetErrorCode;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPetDetailService implements GetPetDetailUseCase {
    private final PetRepository petRepository;

    @Override
    public PetDetailResponse execute(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(PetErrorCode.PET_NOT_FOUND::defaultException);

        Preconditions.validate(pet.getUserId().equals(userId), PetErrorCode.NOT_PET_OWNER);

        return new PetDetailResponse(
                pet.getId(),
                pet.getName(),
                pet.getPetType(),
                pet.getBirthDate(),
                pet.getGender(),
                pet.getWeight(),
                pet.getCreatedAt()
        );
    }
}

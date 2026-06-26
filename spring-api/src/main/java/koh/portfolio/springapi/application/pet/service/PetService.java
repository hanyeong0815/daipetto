package koh.portfolio.springapi.application.pet.service;

import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetRequest;
import koh.portfolio.springapi.application.pet.dto.PetDto.CreatePetResponse;
import koh.portfolio.springapi.application.pet.dto.PetDto.PetSummary;
import koh.portfolio.springapi.application.pet.usecase.CreatePetUseCase;
import koh.portfolio.springapi.application.pet.usecase.GetPetListUseCase;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService implements CreatePetUseCase, GetPetListUseCase {
    private final PetRepository petRepository;

    @Override
    public CreatePetResponse execute(Long userId, CreatePetRequest request) {
        Pet pet = Pet.create(
                userId,
                request.name(),
                request.petType(),
                request.birthDate(),
                request.gender(),
                request.weight()
        );

        Pet savedPet = petRepository.save(pet);

        return CreatePetResponse.builder()
                .petId(savedPet.getId())
                .build();
    }

    @Override
    public List<PetSummary> execute(Long userId) {
        List<Pet> petList = petRepository.findAllByUserId(userId);

        return petList.stream()
                .map(
                        pet -> PetSummary.builder()
                                .id(pet.getId())
                                .name(pet.getName())
                                .petType(pet.getPetType())
                                .weight(pet.getWeight())
                                .build()
                )
                .toList();
    }
}

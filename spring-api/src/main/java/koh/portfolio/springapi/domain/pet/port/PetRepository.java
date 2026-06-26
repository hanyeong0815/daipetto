package koh.portfolio.springapi.domain.pet.port;

import koh.portfolio.springapi.domain.pet.model.Pet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PetRepository {
    Pet save(Pet pet);
    Optional<Pet> findById(Long id);
    List<Pet> findAllByUserId(Long userId);
    Optional<Pet> findByUserIdAndName(Long userId, String name);
    void softDelete(Long id, LocalDateTime deletedAt);
}

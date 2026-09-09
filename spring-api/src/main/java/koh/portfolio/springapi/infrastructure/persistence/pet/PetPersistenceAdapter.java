package koh.portfolio.springapi.infrastructure.persistence.pet;

import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PetPersistenceAdapter implements PetRepository {
    private final PetJpaRepository petJpaRepository;
    private final PetMapper petMapper;

    @Override
    public Pet save(Pet pet) {
        PetEntity petEntity = petMapper.toEntity(pet);

        PetEntity saveEntity = petJpaRepository.save(petEntity);

        return petMapper.toDomain(saveEntity);
    }

    @Override
    public Optional<Pet> findById(Long id) {
        return petJpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(petMapper::toDomain);
    }

    @Override
    public List<Pet> findAllByUserId(Long userId) {
        return petJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId)
                .stream().map(petMapper::toDomain).toList();
    }

    @Override
    public Optional<Pet> findByUserIdAndName(Long userId, String name) {
        return petJpaRepository.findByUserIdAndName(userId, name)
                .map(petMapper::toDomain);
    }


    @Override
    public void softDelete(Long id, LocalDateTime deletedAt) {
        petJpaRepository.softDeleteById(id, deletedAt);
    }
}

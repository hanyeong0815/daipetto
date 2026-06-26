package koh.portfolio.springapi.infrastructure.persistence.pet;

import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface PetMapper extends DomainEntityMapper<Pet, PetEntity> {
}

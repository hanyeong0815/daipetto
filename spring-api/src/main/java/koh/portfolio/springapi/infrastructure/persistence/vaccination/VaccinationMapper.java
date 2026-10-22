package koh.portfolio.springapi.infrastructure.persistence.vaccination;

import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface VaccinationMapper extends DomainEntityMapper<Vaccination, VaccinationEntity> {
}

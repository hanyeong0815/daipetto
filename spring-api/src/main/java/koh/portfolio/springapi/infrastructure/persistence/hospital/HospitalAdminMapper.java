package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalAdmin;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface HospitalAdminMapper extends DomainEntityMapper<HospitalAdmin, HospitalAdminEntity> {
}

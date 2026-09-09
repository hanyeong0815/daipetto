package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface HospitalBusinessHoursMapper extends DomainEntityMapper<HospitalBusinessHours, HospitalBusinessHoursEntity> {
}

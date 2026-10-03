package koh.portfolio.springapi.infrastructure.persistence.healthrecord;

import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import koh.portfolio.springapi.infrastructure.mapper.DomainEntityMapper;
import org.mapstruct.Mapper;

@Mapper
public interface HealthRecordMapper extends DomainEntityMapper<HealthRecord, HealthRecordEntity> {
}

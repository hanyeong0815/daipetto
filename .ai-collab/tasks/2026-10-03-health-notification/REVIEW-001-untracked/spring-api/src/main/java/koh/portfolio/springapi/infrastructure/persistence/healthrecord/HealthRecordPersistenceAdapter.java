package koh.portfolio.springapi.infrastructure.persistence.healthrecord;

import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import koh.portfolio.springapi.domain.healthrecord.port.HealthRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HealthRecordPersistenceAdapter implements HealthRecordRepository {
    private final HealthRecordJpaRepository healthRecordJpaRepository;
    private final HealthRecordMapper healthRecordMapper;

    @Override
    public HealthRecord save(HealthRecord healthRecord) {
        HealthRecordEntity savedEntity = healthRecordJpaRepository.save(healthRecordMapper.toEntity(healthRecord));

        return healthRecordMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<HealthRecord> findById(Long id) {
        return healthRecordJpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(healthRecordMapper::toDomain);
    }

    @Override
    public List<HealthRecord> findAllByPetId(Long petId) {
        return healthRecordJpaRepository.findAllByPetIdAndDeletedAtIsNullOrderByRecordedDateDesc(petId)
                .stream().map(healthRecordMapper::toDomain).toList();
    }

    @Override
    public void softDelete(Long id, LocalDateTime deletedAt) {
        healthRecordJpaRepository.softDeleteById(id, deletedAt);
    }
}

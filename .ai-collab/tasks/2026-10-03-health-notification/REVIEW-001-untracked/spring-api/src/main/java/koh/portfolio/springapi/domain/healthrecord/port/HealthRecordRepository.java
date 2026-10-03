package koh.portfolio.springapi.domain.healthrecord.port;

import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository {
    HealthRecord save(HealthRecord healthRecord);
    Optional<HealthRecord> findById(Long id);
    List<HealthRecord> findAllByPetId(Long petId);
    void softDelete(Long id, LocalDateTime deletedAt);
}

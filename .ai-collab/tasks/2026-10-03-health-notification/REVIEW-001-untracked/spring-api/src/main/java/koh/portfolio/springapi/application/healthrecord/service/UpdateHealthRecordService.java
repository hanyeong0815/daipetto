package koh.portfolio.springapi.application.healthrecord.service;

import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.UpdateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.usecase.UpdateHealthRecordUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.healthrecord.exception.HealthRecordErrorCode;
import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import koh.portfolio.springapi.domain.healthrecord.port.HealthRecordRepository;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateHealthRecordService implements UpdateHealthRecordUseCase {
    private final HealthRecordRepository healthRecordRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public void execute(Long userId, Long healthRecordId, UpdateHealthRecordRequest request) {
        HealthRecord healthRecord = healthRecordRepository.findById(healthRecordId)
                .orElseThrow(HealthRecordErrorCode.HEALTH_RECORD_NOT_FOUND::defaultException);

        Pet pet = petRepository.findById(healthRecord.getPetId()).orElse(null);
        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                HealthRecordErrorCode.NOT_PET_OWNER
        );

        HealthRecord updatedHealthRecord = healthRecord.update(
                request.weight(),
                request.symptom(),
                request.memo(),
                request.recordedDate() != null ? request.recordedDate() : healthRecord.getRecordedDate()
        );

        healthRecordRepository.save(updatedHealthRecord);
    }
}

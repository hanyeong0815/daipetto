package koh.portfolio.springapi.application.healthrecord.service;

import koh.portfolio.springapi.application.healthrecord.usecase.DeleteHealthRecordUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.healthrecord.exception.HealthRecordErrorCode;
import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import koh.portfolio.springapi.domain.healthrecord.port.HealthRecordRepository;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteHealthRecordService implements DeleteHealthRecordUseCase {
    private final HealthRecordRepository healthRecordRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public void execute(Long userId, Long healthRecordId) {
        HealthRecord healthRecord = healthRecordRepository.findById(healthRecordId)
                .orElseThrow(HealthRecordErrorCode.HEALTH_RECORD_NOT_FOUND::defaultException);

        Pet pet = petRepository.findById(healthRecord.getPetId()).orElse(null);
        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                HealthRecordErrorCode.NOT_PET_OWNER
        );

        healthRecordRepository.softDelete(healthRecordId, LocalDateTime.now());
    }
}

package koh.portfolio.springapi.application.healthrecord.service;

import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.HealthRecordResponse;
import koh.portfolio.springapi.application.healthrecord.usecase.CreateHealthRecordUseCase;
import koh.portfolio.springapi.application.healthrecord.usecase.GetHealthRecordListUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.healthrecord.exception.HealthRecordErrorCode;
import koh.portfolio.springapi.domain.healthrecord.model.HealthRecord;
import koh.portfolio.springapi.domain.healthrecord.port.HealthRecordRepository;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthRecordService implements CreateHealthRecordUseCase, GetHealthRecordListUseCase {
    private final HealthRecordRepository healthRecordRepository;
    private final PetRepository petRepository;

    @Override
    @Transactional
    public CreateHealthRecordResponse execute(Long userId, Long petId, CreateHealthRecordRequest request) {
        validatePetOwner(userId, petId);

        HealthRecord healthRecord = HealthRecord.create(
                petId,
                request.weight(),
                request.symptom(),
                request.memo(),
                request.recordedDate() != null ? request.recordedDate() : LocalDate.now()
        );

        HealthRecord savedHealthRecord = healthRecordRepository.save(healthRecord);

        return CreateHealthRecordResponse.builder()
                .healthRecordId(savedHealthRecord.getId())
                .build();
    }

    @Override
    public List<HealthRecordResponse> execute(Long userId, Long petId) {
        validatePetOwner(userId, petId);

        return healthRecordRepository.findAllByPetId(petId).stream()
                .map(healthRecord -> HealthRecordResponse.builder()
                        .healthRecordId(healthRecord.getId())
                        .weight(healthRecord.getWeight())
                        .symptom(healthRecord.getSymptom())
                        .memo(healthRecord.getMemo())
                        .recordedDate(healthRecord.getRecordedDate())
                        .build())
                .toList();
    }

    // 健康記録はペット経由で所有者を判定する（記録自体はuserIdを持たない）
    private void validatePetOwner(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId).orElse(null);

        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                HealthRecordErrorCode.NOT_PET_OWNER
        );
    }
}

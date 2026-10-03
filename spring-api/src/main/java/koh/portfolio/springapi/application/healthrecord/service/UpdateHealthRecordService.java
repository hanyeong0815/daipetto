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
        // 行ロックで読み、同じ記録への他の変更（別PATCH・論理削除）がcommitされた後の最新値に部分更新を当てる（REVIEW-002 R-04）
        HealthRecord healthRecord = healthRecordRepository.findByIdForUpdate(healthRecordId)
                .orElseThrow(HealthRecordErrorCode.HEALTH_RECORD_NOT_FOUND::defaultException);

        Pet pet = petRepository.findById(healthRecord.getPetId()).orElse(null);
        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                HealthRecordErrorCode.NOT_PET_OWNER
        );

        HealthRecord patchedHealthRecord = healthRecord.patch(
                request.weight(),
                request.symptom(),
                request.memo(),
                request.recordedDate()
        );

        // 削除済み行を復活させない条件付きUPDATE。ロック取得後は通常1件だが、0件なら削除を最終状態として維持する
        Preconditions.validate(
                healthRecordRepository.updateContent(patchedHealthRecord),
                HealthRecordErrorCode.HEALTH_RECORD_NOT_FOUND
        );
    }
}

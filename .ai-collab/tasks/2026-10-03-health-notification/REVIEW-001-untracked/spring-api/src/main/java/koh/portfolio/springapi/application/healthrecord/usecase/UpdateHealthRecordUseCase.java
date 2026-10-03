package koh.portfolio.springapi.application.healthrecord.usecase;

import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.UpdateHealthRecordRequest;

public interface UpdateHealthRecordUseCase {
    void execute(Long userId, Long healthRecordId, UpdateHealthRecordRequest request);
}

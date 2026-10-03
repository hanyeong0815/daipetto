package koh.portfolio.springapi.application.healthrecord.usecase;

import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordRequest;
import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.CreateHealthRecordResponse;

public interface CreateHealthRecordUseCase {
    CreateHealthRecordResponse execute(Long userId, Long petId, CreateHealthRecordRequest request);
}

package koh.portfolio.springapi.application.healthrecord.usecase;

import koh.portfolio.springapi.application.healthrecord.dto.HealthRecordDto.HealthRecordResponse;

import java.util.List;

public interface GetHealthRecordListUseCase {
    List<HealthRecordResponse> execute(Long userId, Long petId);
}

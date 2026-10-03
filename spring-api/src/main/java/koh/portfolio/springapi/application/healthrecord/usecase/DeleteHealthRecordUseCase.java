package koh.portfolio.springapi.application.healthrecord.usecase;

public interface DeleteHealthRecordUseCase {
    void execute(Long userId, Long healthRecordId);
}

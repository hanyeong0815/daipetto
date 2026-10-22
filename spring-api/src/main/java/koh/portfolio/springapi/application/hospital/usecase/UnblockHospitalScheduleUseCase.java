package koh.portfolio.springapi.application.hospital.usecase;

public interface UnblockHospitalScheduleUseCase {
    void execute(Long userId, Long hospitalId, Long scheduleId);
}

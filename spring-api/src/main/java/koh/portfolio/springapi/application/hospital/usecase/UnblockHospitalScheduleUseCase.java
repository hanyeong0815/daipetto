package koh.portfolio.springapi.application.hospital.usecase;

public interface UnblockHospitalScheduleUseCase {
    void execute(Long hospitalId, Long scheduleId);
}

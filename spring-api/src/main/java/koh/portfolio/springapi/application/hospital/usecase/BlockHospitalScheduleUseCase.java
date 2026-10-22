package koh.portfolio.springapi.application.hospital.usecase;

public interface BlockHospitalScheduleUseCase {
    void execute(Long userId, Long hospitalId, Long scheduleId);
}

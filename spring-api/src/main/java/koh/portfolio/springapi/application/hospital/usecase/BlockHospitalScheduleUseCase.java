package koh.portfolio.springapi.application.hospital.usecase;

public interface BlockHospitalScheduleUseCase {
    void execute(Long hospitalId, Long scheduleId);
}

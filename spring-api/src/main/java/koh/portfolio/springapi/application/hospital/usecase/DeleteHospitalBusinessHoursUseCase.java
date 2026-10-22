package koh.portfolio.springapi.application.hospital.usecase;

public interface DeleteHospitalBusinessHoursUseCase {
    void execute(Long userId, Long hospitalId, Long businessHoursId);
}

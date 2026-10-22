package koh.portfolio.springapi.application.hospital.usecase;

public interface AssignHospitalAdminUseCase {
    void execute(Long hospitalId, Long userId);
}

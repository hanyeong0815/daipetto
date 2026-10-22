package koh.portfolio.springapi.application.hospital.usecase;

public interface UnassignHospitalAdminUseCase {
    void execute(Long hospitalId, Long userId);
}

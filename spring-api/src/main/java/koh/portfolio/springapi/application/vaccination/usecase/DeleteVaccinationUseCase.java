package koh.portfolio.springapi.application.vaccination.usecase;

public interface DeleteVaccinationUseCase {
    void execute(Long userId, Long vaccinationId);
}

package koh.portfolio.springapi.application.vaccination.usecase;

import java.time.LocalDate;

public interface CompleteVaccinationUseCase {
    void execute(Long userId, Long vaccinationId, LocalDate completedDate);
}

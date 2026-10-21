package koh.portfolio.springapi.application.vaccination.usecase;

import java.time.LocalDate;

public interface RescheduleVaccinationUseCase {
    void execute(Long userId, Long vaccinationId, LocalDate scheduledDate);
}

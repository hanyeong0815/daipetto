package koh.portfolio.springapi.application.vaccination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import lombok.Builder;

import java.time.LocalDate;

public record VaccinationDto() {
    public record CreateVaccinationRequest(
            @NotBlank
            @Size(max = 100)
            String vaccineName,
            @NotNull
            LocalDate scheduledDate
    ) {}

    @Builder
    public record CreateVaccinationResponse(
            Long vaccinationId
    ) {}

    public record CompleteVaccinationRequest(
            // 省略時は当日
            @PastOrPresent
            LocalDate completedDate
    ) {}

    public record RescheduleVaccinationRequest(
            @NotNull
            LocalDate scheduledDate
    ) {}

    @Builder
    public record VaccinationResponse(
            Long vaccinationId,
            String vaccineName,
            LocalDate scheduledDate,
            LocalDate completedDate,
            VaccinationStatus status
    ) {}
}

package koh.portfolio.springapi.application.hospital.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record HospitalBusinessHoursDto() {
    public record CreateHospitalBusinessHoursRequest(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime openTime,
            @NotNull LocalTime closeTime,
            LocalTime breakStartTime,
            LocalTime breakEndTime,
            @NotNull Integer slotDurationMinutes
    ) {}

    @Builder
    public record CreateHospitalBusinessHoursResponse(Long businessHoursId) {}

    @Builder
    public record HospitalBusinessHoursResponse(
            Long businessHoursId,
            DayOfWeek dayOfWeek,
            LocalTime openTime,
            LocalTime closeTime,
            LocalTime breakStartTime,
            LocalTime breakEndTime,
            Integer slotDurationMinutes
    ) {}

    public record UpdateHospitalBusinessHoursRequest(
            @NotNull LocalTime openTime,
            @NotNull LocalTime closeTime,
            LocalTime breakStartTime,
            LocalTime breakEndTime,
            @NotNull Integer slotDurationMinutes
    ) {}
}

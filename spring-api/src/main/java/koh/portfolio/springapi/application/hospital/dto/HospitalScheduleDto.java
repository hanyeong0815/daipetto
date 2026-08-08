package koh.portfolio.springapi.application.hospital.dto;

import jakarta.validation.constraints.NotNull;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

public record HospitalScheduleDto() {
    @Builder
    public record HospitalScheduleResponse(
            Long scheduleId,
            LocalDate availableDate,
            LocalTime startTime,
            LocalTime endTime,
            HospitalScheduleStatus status
    ) {}

    public record CreateHospitalScheduleRequest(
            @NotNull
            LocalDate availableDate,
            @NotNull
            LocalTime startTime,
            @NotNull
            LocalTime endTime
    ) {}

    @Builder
    public record CreateHospitalScheduleResponse(
            Long scheduleId
    ) {}
}

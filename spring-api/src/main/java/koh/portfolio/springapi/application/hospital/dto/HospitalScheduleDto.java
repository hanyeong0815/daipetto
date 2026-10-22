package koh.portfolio.springapi.application.hospital.dto;

import jakarta.validation.constraints.NotNull;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

public record HospitalScheduleDto() {
    // 予約枠の自動生成（手動実行）。month は "2026-11" 形式
    public record GenerateHospitalSchedulesRequest(
            @NotNull YearMonth month
    ) {}

    // 作成した枠数、枠を作った日数、既存の枠があったため飛ばした日数（休診日は含まない）
    public record GenerateHospitalSchedulesResponse(
            int createdSlots,
            int generatedDates,
            int skippedDates
    ) {}

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

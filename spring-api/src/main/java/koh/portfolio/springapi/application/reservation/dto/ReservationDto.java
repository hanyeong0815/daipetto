package koh.portfolio.springapi.application.reservation.dto;

import jakarta.validation.constraints.NotNull;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.reservation.model.ReservationStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationDto() {
    public record CreateReservationRequest(
            @NotNull
            Long petId,
            @NotNull
            Long hospitalId,
            @NotNull
            Long scheduleId,
            String memo
    ) {}

    @Builder
    public record CreateReservationResponse(
            Long reservationId,
            ReservationStatus status
    ) {}

    @Builder
    public record ReservationSummary(
            Long reservationId,
            String hospitalName,
            String petName,
            LocalDate availableDate,
            LocalTime startTime,
            LocalTime endTime,
            ReservationStatus status
    ) {}

    @Builder
    public record ReservationDetailResponse(
            Long reservationId,
            String hospitalName,
            String petName,
            LocalDate availableDate,
            LocalTime startTime,
            LocalTime endTime,
            ReservationStatus status,
            String memo
    ) {}

    // 病院管理者向け予約一覧（SC-013）
    @Builder
    public record AdminReservationResponse(
            Long reservationId,
            ReservationStatus status,
            LocalDate availableDate,
            LocalTime startTime,
            LocalTime endTime,
            Long userId,
            String userNickname,
            String petName,
            PetType petType,
            String memo,
            LocalDateTime createdAt
    ) {}
}

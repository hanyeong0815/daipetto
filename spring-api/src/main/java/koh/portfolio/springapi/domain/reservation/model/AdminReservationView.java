package koh.portfolio.springapi.domain.reservation.model;

import koh.portfolio.springapi.domain.pet.model.PetType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 病院管理者向け予約一覧（SC-013）の1件。予約・予約枠・予約者・ペットを結合した読み取り専用の値
public record AdminReservationView(
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

package koh.portfolio.springapi.application.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import lombok.Builder;

import java.time.LocalDateTime;

public record HospitalDto() {
    public record CreateHospitalRequest(
            @NotBlank String name,
            @NotBlank String address,
            String phoneNumber
    ) {}

    @Builder
    public record CreateHospitalResponse(Long hospitalId) {}

    @Builder
    public record HospitalSummary(Long id, String name, String address, String phoneNumber) {}

    public record HospitalDetailResponse(
            Long id,
            String name,
            String address,
            String phoneNumber,
            HospitalStatus status,
            LocalDateTime createdAt
    ) {}

    public record UpdateHospitalRequest(
            @NotBlank String name,
            @NotBlank String address,
            String phoneNumber
    ) {}
}

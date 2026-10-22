package koh.portfolio.springapi.application.hospital.dto;

import jakarta.validation.constraints.NotNull;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import lombok.Builder;

public record HospitalAdminDto() {
    // 管理画面で操作できる病院（停止中も含む）
    @Builder
    public record ManagedHospitalResponse(
            Long hospitalId,
            String name,
            String address,
            String phoneNumber,
            HospitalStatus status
    ) {}

    public record HospitalAdminResponse(
            Long userId,
            String email,
            String nickname
    ) {}

    public record AssignHospitalAdminRequest(
            @NotNull Long userId
    ) {}
}

package koh.portfolio.springapi.domain.hospital.model;

// 病院に紐付いた管理者の表示用情報
public record HospitalAdminSummary(
        Long userId,
        String email,
        String nickname
) {}

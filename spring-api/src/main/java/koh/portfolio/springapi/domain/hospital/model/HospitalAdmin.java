package koh.portfolio.springapi.domain.hospital.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// 病院管理者（ROLE_HOSPITAL_ADMIN）と管理対象病院の紐付け（06_ERD §11）
@Getter
@AllArgsConstructor
public class HospitalAdmin {
    private Long id;
    private Long userId;
    private Long hospitalId;
    private LocalDateTime createdAt;

    public static HospitalAdmin link(Long userId, Long hospitalId) {
        return new HospitalAdmin(null, userId, hospitalId, LocalDateTime.now());
    }
}

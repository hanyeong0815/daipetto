package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.auth.exception.AuthErrorCode;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 病院単位の管理操作の権限確認（06_ERD §11 hospital_admins）。
// システム管理者はすべての病院、病院管理者は紐付いた病院だけを管理できる。
// 権限は現在の値で判定する（JwtAuthenticationFilter と同じく、権限変更を即時に反映する）
@Component
@RequiredArgsConstructor
public class HospitalAccessChecker {
    private final UserRepository userRepository;
    private final HospitalAdminRepository hospitalAdminRepository;

    public void checkManageable(Long userId, Long hospitalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(AuthErrorCode.ACCESS_DENIED::defaultException);

        if (user.getRole() == Role.ROLE_SYSTEM_ADMIN) {
            return;
        }

        Preconditions.validate(
                user.getRole() == Role.ROLE_HOSPITAL_ADMIN && hospitalAdminRepository.exists(userId, hospitalId),
                HospitalErrorCode.NOT_HOSPITAL_MANAGER
        );
    }

    public boolean isSystemAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == Role.ROLE_SYSTEM_ADMIN)
                .orElse(false);
    }
}

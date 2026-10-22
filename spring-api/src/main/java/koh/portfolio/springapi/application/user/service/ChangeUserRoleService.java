package koh.portfolio.springapi.application.user.service;

import koh.portfolio.springapi.application.user.usecase.ChangeUserRoleUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChangeUserRoleService implements ChangeUserRoleUseCase {
    private final UserRepository userRepository;
    private final HospitalAdminRepository hospitalAdminRepository;

    @Override
    @Transactional
    public void execute(Long adminId, Long userId, Role role) {
        // 自分の権限を下げるとシステム管理者が居なくなり得るため禁止する
        Preconditions.validate(!adminId.equals(userId), UserErrorCode.CANNOT_CHANGE_SELF);

        // JwtAuthenticationFilter は現在の権限で認証するため、新しい権限は次のリクエストから反映される
        Preconditions.validate(
                userRepository.updateRole(userId, role, LocalDateTime.now()),
                UserErrorCode.NO_SUCH_USER
        );

        // 病院管理者でなくなったユーザーの病院紐付けを外す（再び病院管理者にした場合は改めて紐付ける）
        if (role != Role.ROLE_HOSPITAL_ADMIN) {
            hospitalAdminRepository.deleteAllByUserId(userId);
        }
    }
}

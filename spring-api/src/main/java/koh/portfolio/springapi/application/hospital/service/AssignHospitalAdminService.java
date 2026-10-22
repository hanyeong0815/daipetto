package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.usecase.AssignHospitalAdminUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalAdmin;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.user.exception.UserErrorCode;
import koh.portfolio.springapi.domain.user.model.Role;
import koh.portfolio.springapi.domain.user.model.User;
import koh.portfolio.springapi.domain.user.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignHospitalAdminService implements AssignHospitalAdminUseCase {
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final HospitalAdminRepository hospitalAdminRepository;

    @Override
    @Transactional
    public void execute(Long hospitalId, Long userId) {
        Preconditions.validate(hospitalRepository.findById(hospitalId).isPresent(), HospitalErrorCode.HOSPITAL_NOT_FOUND);

        // 権限変更（UPDATE users）と直列化してから権限を読む。これが無いと、権限確認の直後に病院管理者から外され
        // 紐付けが一括削除された後で、この紐付けだけが一般ユーザーに残る
        userRepository.lockForSessionUpdate(userId);
        User user = userRepository.findById(userId)
                .filter(found -> found.getDeletedAt() == null)
                .orElseThrow(UserErrorCode.NO_SUCH_USER::defaultException);
        Preconditions.validate(user.getRole() == Role.ROLE_HOSPITAL_ADMIN, HospitalErrorCode.NOT_HOSPITAL_ADMIN_USER);

        Preconditions.validate(!hospitalAdminRepository.exists(userId, hospitalId), HospitalErrorCode.HOSPITAL_ADMIN_DUPLICATED);
        // 同時に同じ紐付けを登録した場合は、一意制約で負けた側をアダプターが HOSPITAL-007 に変換する
        hospitalAdminRepository.save(HospitalAdmin.link(userId, hospitalId));
    }
}

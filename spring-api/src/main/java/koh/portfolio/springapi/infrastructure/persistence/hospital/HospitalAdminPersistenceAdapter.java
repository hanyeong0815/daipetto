package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalAdmin;
import koh.portfolio.springapi.domain.hospital.model.HospitalAdminSummary;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class HospitalAdminPersistenceAdapter implements HospitalAdminRepository {
    private static final String UNIQUE_LINK_CONSTRAINT = "uq_hospital_admins_user_hospital";

    private final HospitalAdminJpaRepository hospitalAdminJpaRepository;
    private final HospitalAdminMapper hospitalAdminMapper;

    @Override
    public boolean exists(Long userId, Long hospitalId) {
        return hospitalAdminJpaRepository.existsByUserIdAndHospitalId(userId, hospitalId);
    }

    @Override
    public HospitalAdmin save(HospitalAdmin hospitalAdmin) {
        try {
            // 一意制約違反をここで検知するためflushまで行う（commit時だとSERVER-001になる）
            return hospitalAdminMapper.toDomain(
                    hospitalAdminJpaRepository.saveAndFlush(hospitalAdminMapper.toEntity(hospitalAdmin)));
        } catch (DataIntegrityViolationException e) {
            // 紐付けの一意制約だけを重複として扱う（FK違反などの原因を隠さない）
            if (!isUniqueLinkViolation(e)) {
                throw e;
            }
            throw HospitalErrorCode.HOSPITAL_ADMIN_DUPLICATED.defaultException(e);
        }
    }

    private boolean isUniqueLinkViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException violation
                && UNIQUE_LINK_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
            return true;
        }
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase().contains(UNIQUE_LINK_CONSTRAINT);
    }

    @Override
    public boolean delete(Long userId, Long hospitalId) {
        return hospitalAdminJpaRepository.deleteLink(userId, hospitalId) == 1;
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        hospitalAdminJpaRepository.deleteAllByUser(userId);
    }

    @Override
    public List<Long> findHospitalIdsByUserId(Long userId) {
        return hospitalAdminJpaRepository.findHospitalIdsByUserId(userId);
    }

    @Override
    public List<HospitalAdminSummary> findAdminsByHospitalId(Long hospitalId) {
        return hospitalAdminJpaRepository.findAdminsByHospitalId(hospitalId);
    }
}

package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.usecase.UnassignHospitalAdminUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnassignHospitalAdminService implements UnassignHospitalAdminUseCase {
    private final HospitalAdminRepository hospitalAdminRepository;

    @Override
    @Transactional
    public void execute(Long hospitalId, Long userId) {
        Preconditions.validate(hospitalAdminRepository.delete(userId, hospitalId), HospitalErrorCode.HOSPITAL_ADMIN_NOT_FOUND);
    }
}

package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.usecase.SuspendHospitalUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuspendHospitalService implements SuspendHospitalUseCase {
    private final HospitalRepository hospitalRepository;

    @Override
    @Transactional
    public void execute(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(HospitalErrorCode.HOSPITAL_NOT_FOUND::defaultException);

        Hospital suspendedHospital = hospital.suspend(HospitalStatus.SUSPENDED);

        // status列だけを書く。同時に行われた情報更新を古い値で上書きしない
        Preconditions.validate(
                hospitalRepository.updateStatus(hospitalId, suspendedHospital.getStatus(), suspendedHospital.getUpdatedAt()),
                HospitalErrorCode.HOSPITAL_NOT_FOUND
        );
    }
}

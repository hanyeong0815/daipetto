package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.usecase.SuspendHospitalUseCase;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuspendHospitalService implements SuspendHospitalUseCase {
    private final HospitalRepository hospitalRepository;

    @Override
    public void execute(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(HospitalErrorCode.HOSPITAL_NOT_FOUND::defaultException);

        Hospital suspendedHospital = hospital.suspend(HospitalStatus.SUSPENDED);

        hospitalRepository.save(suspendedHospital);
    }
}

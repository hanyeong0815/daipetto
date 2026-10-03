package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.UpdateHospitalRequest;
import koh.portfolio.springapi.application.hospital.usecase.UpdateHospitalUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateHospitalService implements UpdateHospitalUseCase {
    private final HospitalRepository hospitalRepository;

    @Override
    @Transactional
    public void execute(Long hospitalId, UpdateHospitalRequest request) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(HospitalErrorCode.HOSPITAL_NOT_FOUND::defaultException);

        Hospital updatedHospital = hospital.update(
                request.name(),
                request.address(),
                request.phoneNumber()
        );

        // 情報列だけを書く。statusは触らないため、同時に行われた停止を取り消さない
        Preconditions.validate(
                hospitalRepository.updateInfo(updatedHospital),
                HospitalErrorCode.HOSPITAL_NOT_FOUND
        );
    }
}

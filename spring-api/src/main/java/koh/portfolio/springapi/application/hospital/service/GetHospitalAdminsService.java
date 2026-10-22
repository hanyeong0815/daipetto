package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.HospitalAdminResponse;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalAdminsUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetHospitalAdminsService implements GetHospitalAdminsUseCase {
    private final HospitalRepository hospitalRepository;
    private final HospitalAdminRepository hospitalAdminRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HospitalAdminResponse> execute(Long hospitalId) {
        Preconditions.validate(hospitalRepository.findById(hospitalId).isPresent(), HospitalErrorCode.HOSPITAL_NOT_FOUND);

        return hospitalAdminRepository.findAdminsByHospitalId(hospitalId).stream()
                .map(admin -> new HospitalAdminResponse(admin.userId(), admin.email(), admin.nickname()))
                .toList();
    }
}

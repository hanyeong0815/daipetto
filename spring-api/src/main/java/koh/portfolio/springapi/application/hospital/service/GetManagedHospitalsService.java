package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalAdminDto.ManagedHospitalResponse;
import koh.portfolio.springapi.application.hospital.usecase.GetManagedHospitalsUseCase;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.port.HospitalAdminRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetManagedHospitalsService implements GetManagedHospitalsUseCase {
    private final HospitalAccessChecker hospitalAccessChecker;
    private final HospitalRepository hospitalRepository;
    private final HospitalAdminRepository hospitalAdminRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ManagedHospitalResponse> execute(Long userId) {
        // システム管理者はすべての病院（停止中も含む）、病院管理者は紐付いた病院だけ
        List<Hospital> hospitals = hospitalAccessChecker.isSystemAdmin(userId)
                ? hospitalRepository.findAllForManagement()
                : hospitalRepository.findAllForManagement(hospitalAdminRepository.findHospitalIdsByUserId(userId));

        return hospitals.stream()
                .map(hospital -> ManagedHospitalResponse.builder()
                        .hospitalId(hospital.getId())
                        .name(hospital.getName())
                        .address(hospital.getAddress())
                        .phoneNumber(hospital.getPhoneNumber())
                        .status(hospital.getStatus())
                        .build())
                .toList();
    }
}

package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalDetailResponse;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalDetailUseCase;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetHospitalDetailService implements GetHospitalDetailUseCase {
    private final HospitalRepository hospitalRepository;

    @Override
    public HospitalDetailResponse execute(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(HospitalErrorCode.HOSPITAL_NOT_FOUND::defaultException);

        return new HospitalDetailResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getAddress(),
                hospital.getPhoneNumber(),
                hospital.getStatus(),
                hospital.getCreatedAt()
        );
    }
}

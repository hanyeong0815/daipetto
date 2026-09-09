package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.CreateHospitalResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalDto.HospitalSummary;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalListUseCase;
import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalService implements CreateHospitalUseCase, GetHospitalListUseCase {
    private final HospitalRepository hospitalRepository;

    @Override
    public CreateHospitalResponse execute(CreateHospitalRequest request) {
        Hospital hospital = Hospital.create(
                request.name(),
                request.address(),
                request.phoneNumber()
        );

        Hospital savedHospital = hospitalRepository.save(hospital);

        return CreateHospitalResponse.builder()
                .hospitalId(savedHospital.getId())
                .build();
    }

    @Override
    public List<HospitalSummary> execute(String keyword, String area) {
        List<Hospital> hospitals = hospitalRepository.search(keyword, area);

        return hospitals.stream()
                .map(
                        hospital -> HospitalSummary.builder()
                                .id(hospital.getId())
                                .name(hospital.getName())
                                .address(hospital.getAddress())
                                .phoneNumber(hospital.getPhoneNumber())
                                .build()
                )
                .toList();
    }
}

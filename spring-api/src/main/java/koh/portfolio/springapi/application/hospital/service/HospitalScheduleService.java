package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.CreateHospitalScheduleResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalScheduleDto.HospitalScheduleResponse;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalScheduleUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalScheduleListUseCase;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static koh.portfolio.springapi.common.exception.Preconditions.validate;

@Service
@RequiredArgsConstructor
public class HospitalScheduleService implements GetHospitalScheduleListUseCase, CreateHospitalScheduleUseCase {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Override
    public List<HospitalScheduleResponse> execute(Long hospitalId) {
        validate(hospitalRepository.findById(hospitalId).isPresent(), HospitalErrorCode.HOSPITAL_NOT_FOUND);

        List<HospitalSchedule> schedules = hospitalScheduleRepository.findAllByHospitalId(hospitalId);

        return schedules.stream()
                .map(
                        schedule -> HospitalScheduleResponse.builder()
                                .scheduleId(schedule.getId())
                                .availableDate(schedule.getAvailableDate())
                                .startTime(schedule.getStartTime())
                                .endTime(schedule.getEndTime())
                                .status(schedule.getStatus())
                                .build()
                )
                .toList();
    }

    @Override
    public CreateHospitalScheduleResponse execute(Long hospitalId, CreateHospitalScheduleRequest request) {
        validate(
                hospitalRepository.existsById(hospitalId),
                HospitalErrorCode.HOSPITAL_NOT_FOUND
        );

        HospitalSchedule createHospitalSchedule = HospitalSchedule.create(
                hospitalId,
                request.availableDate(),
                request.startTime(),
                request.endTime()
        );

        HospitalSchedule savedHospitalSchedule = hospitalScheduleRepository.save(createHospitalSchedule);

        return CreateHospitalScheduleResponse.builder()
                .scheduleId(savedHospitalSchedule.getId())
                .build();
    }
}

package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursRequest;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.CreateHospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.dto.HospitalBusinessHoursDto.HospitalBusinessHoursResponse;
import koh.portfolio.springapi.application.hospital.usecase.CreateHospitalBusinessHoursUseCase;
import koh.portfolio.springapi.application.hospital.usecase.GetHospitalBusinessHoursListUseCase;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.domain.hospital.port.HospitalBusinessHoursRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static koh.portfolio.springapi.common.exception.Preconditions.validate;

@Service
@RequiredArgsConstructor
public class HospitalBusinessHoursService implements GetHospitalBusinessHoursListUseCase, CreateHospitalBusinessHoursUseCase {
    private final HospitalRepository hospitalRepository;
    private final HospitalBusinessHoursRepository hospitalBusinessHoursRepository;

    @Override
    public List<HospitalBusinessHoursResponse> execute(Long hospitalId) {
        validate(hospitalRepository.existsById(hospitalId), HospitalErrorCode.HOSPITAL_NOT_FOUND);

        List<HospitalBusinessHours> businessHours = hospitalBusinessHoursRepository.findByHospitalId(hospitalId);

        return businessHours.stream()
                .map(
                        businessHour -> HospitalBusinessHoursResponse.builder()
                                .businessHoursId(businessHour.getId())
                                .dayOfWeek(businessHour.getDayOfWeek())
                                .openTime(businessHour.getOpenTime())
                                .closeTime(businessHour.getCloseTime())
                                .breakStartTime(businessHour.getBreakStartTime())
                                .breakEndTime(businessHour.getBreakEndTime())
                                .slotDurationMinutes(businessHour.getSlotDurationMinutes())
                                .build()
                )
                .toList();
    }

    @Override
    public CreateHospitalBusinessHoursResponse execute(Long hospitalId, CreateHospitalBusinessHoursRequest request) {
        validate(hospitalRepository.existsById(hospitalId), HospitalErrorCode.HOSPITAL_NOT_FOUND);
        validate(
                !hospitalBusinessHoursRepository.existsByHospitalIdAndDayOfWeek(hospitalId, request.dayOfWeek()),
                HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_DUPLICATED
        );

        HospitalBusinessHours createHospitalBusinessHours = HospitalBusinessHours.create(
                hospitalId,
                request.dayOfWeek(),
                request.openTime(),
                request.closeTime(),
                request.breakStartTime(),
                request.breakEndTime(),
                request.slotDurationMinutes()
        );

        HospitalBusinessHours savedHospitalBusinessHours = hospitalBusinessHoursRepository.save(createHospitalBusinessHours);

        return CreateHospitalBusinessHoursResponse.builder()
                .businessHoursId(savedHospitalBusinessHours.getId())
                .build();
    }
}

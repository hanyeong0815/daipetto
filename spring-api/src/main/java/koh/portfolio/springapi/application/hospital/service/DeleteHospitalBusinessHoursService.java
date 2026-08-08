package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.usecase.DeleteHospitalBusinessHoursUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.domain.hospital.port.HospitalBusinessHoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteHospitalBusinessHoursService implements DeleteHospitalBusinessHoursUseCase {
    private final HospitalBusinessHoursRepository hospitalBusinessHoursRepository;

    @Override
    @Transactional
    public void execute(Long hospitalId, Long businessHoursId) {
        HospitalBusinessHours businessHours = hospitalBusinessHoursRepository.findById(businessHoursId)
                .orElseThrow(HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_NOT_FOUND::defaultException);

        Preconditions.validate(businessHours.getHospitalId().equals(hospitalId), HospitalErrorCode.HOSPITAL_BUSINESS_HOURS_NOT_FOUND);

        hospitalBusinessHoursRepository.deleteById(businessHoursId);
    }
}

package koh.portfolio.springapi.application.hospital.service;

import koh.portfolio.springapi.application.hospital.usecase.BlockHospitalScheduleUseCase;
import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.hospital.exception.HospitalErrorCode;
import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockHospitalScheduleService implements BlockHospitalScheduleUseCase {
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Override
    @Transactional
    public void execute(Long hospitalId, Long scheduleId) {
        HospitalSchedule schedule = hospitalScheduleRepository.findById(scheduleId)
                .orElseThrow(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND::defaultException);

        Preconditions.validate(schedule.getHospitalId().equals(hospitalId), HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND);

        hospitalScheduleRepository.save(schedule.block());
    }
}

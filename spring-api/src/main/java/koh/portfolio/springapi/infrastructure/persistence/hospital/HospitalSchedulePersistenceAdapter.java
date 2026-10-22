package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HospitalSchedulePersistenceAdapter implements HospitalScheduleRepository {
    private final HospitalScheduleJpaRepository hospitalScheduleJpaRepository;
    private final HospitalScheduleMapper hospitalScheduleMapper;

    @Override
    public List<HospitalSchedule> findAllByHospitalId(Long hospitalId) {
        return hospitalScheduleJpaRepository.findAllByHospitalIdOrderByAvailableDateAscStartTimeAsc(hospitalId)
                .stream().map(hospitalScheduleMapper::toDomain).toList();
    }

    @Override
    public HospitalSchedule save(HospitalSchedule hospitalSchedule) {
        return hospitalScheduleMapper.toDomain(
                hospitalScheduleJpaRepository.save(
                        hospitalScheduleMapper.toEntity(hospitalSchedule)
                )
        );
    }

    @Override
    public Optional<HospitalSchedule> findById(Long id) {
        return hospitalScheduleJpaRepository.findById(id)
                .map(hospitalScheduleMapper::toDomain);
    }

    @Override
    public List<LocalDate> findDatesWithSchedules(Long hospitalId, LocalDate from, LocalDate to) {
        return hospitalScheduleJpaRepository.findDatesWithSchedules(hospitalId, from, to);
    }

    @Override
    public void saveAll(List<HospitalSchedule> hospitalSchedules) {
        hospitalScheduleJpaRepository.saveAll(hospitalSchedules.stream().map(hospitalScheduleMapper::toEntity).toList());
    }
}

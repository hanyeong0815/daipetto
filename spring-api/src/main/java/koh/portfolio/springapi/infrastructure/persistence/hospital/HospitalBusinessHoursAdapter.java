package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.domain.hospital.port.HospitalBusinessHoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HospitalBusinessHoursAdapter implements HospitalBusinessHoursRepository {
    private final HospitalBusinessHoursJpaRepository hospitalBusinessHoursJpaRepository;
    private final HospitalBusinessHoursMapper hospitalBusinessHoursMapper;


    @Override
    public HospitalBusinessHours save(HospitalBusinessHours hospitalBusinessHours) {
        return hospitalBusinessHoursMapper.toDomain(
                hospitalBusinessHoursJpaRepository.save(
                        hospitalBusinessHoursMapper.toEntity(hospitalBusinessHours)
                )
        );
    }

    @Override
    public Optional<HospitalBusinessHours> findById(Long id) {
        return hospitalBusinessHoursJpaRepository.findById(id)
                .map(hospitalBusinessHoursMapper::toDomain);
    }

    @Override
    public List<HospitalBusinessHours> findByHospitalId(Long hospitalId) {
        return hospitalBusinessHoursJpaRepository.findByHospitalId(hospitalId)
                .stream().map(hospitalBusinessHoursMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByHospitalIdAndDayOfWeek(Long hospitalId, DayOfWeek dayOfWeek) {
        return hospitalBusinessHoursJpaRepository.existsByHospitalIdAndDayOfWeek(hospitalId, dayOfWeek);
    }

    @Override
    public void deleteById(Long id) {
        hospitalBusinessHoursJpaRepository.deleteById(id);
    }
}

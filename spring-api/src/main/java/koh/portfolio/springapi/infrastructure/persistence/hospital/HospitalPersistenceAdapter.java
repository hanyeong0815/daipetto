package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.Hospital;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HospitalPersistenceAdapter implements HospitalRepository {
    private final HospitalJpaRepository hospitalJpaRepository;
    private final HospitalMapper hospitalMapper;

    @Override
    public Hospital save(Hospital hospital) {
        HospitalEntity entity = hospitalMapper.toEntity(hospital);

        HospitalEntity savedEntity = hospitalJpaRepository.save(entity);

        return hospitalMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Hospital> findById(Long id) {
        return hospitalJpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(hospitalMapper::toDomain);
    }

    @Override
    public List<Hospital> search(String keyword, String area) {
        return hospitalJpaRepository.search(HospitalStatus.ACTIVE, keyword, area)
                .stream().map(hospitalMapper::toDomain).toList();
    }

    @Override
    public boolean existsById(Long id) {
        return hospitalJpaRepository.existsById(id);
    }
}

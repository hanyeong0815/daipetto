package koh.portfolio.springapi.infrastructure.persistence.hospital;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalScheduleJpaRepository extends JpaRepository<HospitalScheduleEntity, Long> {
    List<HospitalScheduleEntity> findAllByHospitalIdOrderByAvailableDateAscStartTimeAsc(Long hospitalId);
}

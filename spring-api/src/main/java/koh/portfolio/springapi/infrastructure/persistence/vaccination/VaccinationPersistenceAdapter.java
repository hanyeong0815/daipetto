package koh.portfolio.springapi.infrastructure.persistence.vaccination;

import koh.portfolio.springapi.domain.vaccination.model.Vaccination;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationReminderTarget;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VaccinationPersistenceAdapter implements VaccinationRepository {
    private final VaccinationJpaRepository vaccinationJpaRepository;
    private final VaccinationMapper vaccinationMapper;

    @Override
    public Vaccination save(Vaccination vaccination) {
        VaccinationEntity savedEntity = vaccinationJpaRepository.save(vaccinationMapper.toEntity(vaccination));

        return vaccinationMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Vaccination> findById(Long id) {
        return vaccinationJpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(vaccinationMapper::toDomain);
    }

    @Override
    public List<Vaccination> findAllByPetId(Long petId) {
        return vaccinationJpaRepository.findAllByPetIdAndDeletedAtIsNullOrderByScheduledDateAsc(petId)
                .stream().map(vaccinationMapper::toDomain).toList();
    }

    @Override
    public boolean markCompleted(Long id, LocalDate completedDate, LocalDateTime updatedAt) {
        return vaccinationJpaRepository.markCompleted(
                id, VaccinationStatus.SCHEDULED, VaccinationStatus.COMPLETED, completedDate, updatedAt) == 1;
    }

    @Override
    public boolean reschedule(Long id, LocalDate scheduledDate, LocalDateTime updatedAt) {
        return vaccinationJpaRepository.reschedule(
                id, VaccinationStatus.CANCELLED, VaccinationStatus.SCHEDULED, scheduledDate, updatedAt) == 1;
    }

    @Override
    public boolean cancel(Long id, LocalDateTime updatedAt) {
        return vaccinationJpaRepository.cancel(
                id, VaccinationStatus.SCHEDULED, VaccinationStatus.CANCELLED, updatedAt) == 1;
    }

    @Override
    public boolean markReminded(Long id, LocalDate scheduledDate) {
        return vaccinationJpaRepository.markReminded(id, VaccinationStatus.SCHEDULED, scheduledDate) == 1;
    }

    @Override
    public boolean softDelete(Long id, LocalDateTime deletedAt) {
        return vaccinationJpaRepository.softDelete(id, deletedAt) == 1;
    }

    @Override
    public List<VaccinationReminderTarget> findReminderTargets(LocalDate from, LocalDate to) {
        return vaccinationJpaRepository.findReminderTargets(VaccinationStatus.SCHEDULED, from, to);
    }
}

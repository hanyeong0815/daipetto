package koh.portfolio.springapi.infrastructure.persistence.vaccination;

import koh.portfolio.springapi.domain.pet.model.PetGender;
import koh.portfolio.springapi.domain.pet.model.PetType;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationReminderTarget;
import koh.portfolio.springapi.domain.vaccination.model.VaccinationStatus;
import koh.portfolio.springapi.domain.vaccination.port.VaccinationRepository;
import koh.portfolio.springapi.infrastructure.persistence.pet.PetEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        VaccinationPersistenceAdapter.class,
        VaccinationMapperImpl.class
})
class VaccinationPersistenceAdapterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Autowired
    private VaccinationRepository vaccinationRepository;

    @Autowired
    private VaccinationJpaRepository vaccinationJpaRepository;

    @Autowired
    private TestEntityManager em;

    private PetEntity persistPet(Long userId, LocalDateTime deletedAt) {
        return em.persist(PetEntity.builder()
                .userId(userId)
                .name("Momo")
                .petType(PetType.CAT)
                .gender(PetGender.FEMALE)
                .createdAt(CHANGED_AT)
                .updatedAt(CHANGED_AT)
                .deletedAt(deletedAt)
                .build());
    }

    private VaccinationEntity persistVaccination(Long petId, VaccinationStatus status, LocalDate scheduledDate) {
        return vaccinationJpaRepository.save(VaccinationEntity.builder()
                .petId(petId)
                .vaccineName("混合ワクチン")
                .scheduledDate(scheduledDate)
                .status(status)
                .createdAt(CHANGED_AT)
                .updatedAt(CHANGED_AT)
                .build());
    }

    private List<Long> reminderTargetIds() {
        em.flush();
        return vaccinationRepository.findReminderTargets(TODAY, TODAY.plusDays(7)).stream()
                .map(VaccinationReminderTarget::vaccinationId)
                .toList();
    }

    @Test
    @DisplayName("接種完了はSCHEDULEDの行だけを更新する")
    void mark_completed_updates_only_scheduled_row() {
        // given
        Long petId = persistPet(1L, null).getId();
        VaccinationEntity scheduled = persistVaccination(petId, VaccinationStatus.SCHEDULED, TODAY);
        VaccinationEntity cancelled = persistVaccination(petId, VaccinationStatus.CANCELLED, TODAY);

        // when
        boolean completedScheduled = vaccinationRepository.markCompleted(scheduled.getId(), TODAY, LocalDateTime.now());
        boolean completedCancelled = vaccinationRepository.markCompleted(cancelled.getId(), TODAY, LocalDateTime.now());

        // then
        assertThat(completedScheduled).isTrue();
        assertThat(completedCancelled).isFalse();
        VaccinationEntity reloaded = vaccinationJpaRepository.findById(scheduled.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(VaccinationStatus.COMPLETED);
        assertThat(reloaded.getCompletedDate()).isEqualTo(TODAY);
        assertThat(vaccinationJpaRepository.findById(cancelled.getId()).orElseThrow().getStatus())
                .isEqualTo(VaccinationStatus.CANCELLED);
    }

    @Test
    @DisplayName("再登録はCANCELLEDの行だけを新しい予定日でSCHEDULEDに戻す")
    void reschedule_updates_only_cancelled_row() {
        // given
        Long petId = persistPet(1L, null).getId();
        VaccinationEntity cancelled = persistVaccination(petId, VaccinationStatus.CANCELLED, TODAY);
        VaccinationEntity completed = persistVaccination(petId, VaccinationStatus.COMPLETED, TODAY);
        LocalDate newDate = TODAY.plusDays(30);

        // when & then
        assertThat(vaccinationRepository.reschedule(cancelled.getId(), newDate, LocalDateTime.now())).isTrue();
        assertThat(vaccinationRepository.reschedule(completed.getId(), newDate, LocalDateTime.now())).isFalse();

        VaccinationEntity reloaded = vaccinationJpaRepository.findById(cancelled.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(VaccinationStatus.SCHEDULED);
        assertThat(reloaded.getScheduledDate()).isEqualTo(newDate);
        assertThat(vaccinationJpaRepository.findById(completed.getId()).orElseThrow().getScheduledDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("中止はSCHEDULEDの行だけをCANCELLEDにし、updated_atを進める")
    void cancel_updates_only_scheduled_row() {
        // given
        Long petId = persistPet(1L, null).getId();
        VaccinationEntity scheduled = persistVaccination(petId, VaccinationStatus.SCHEDULED, TODAY);
        VaccinationEntity completed = persistVaccination(petId, VaccinationStatus.COMPLETED, TODAY);
        LocalDateTime cancelledAt = CHANGED_AT.plusDays(1);

        // when & then
        assertThat(vaccinationRepository.cancel(scheduled.getId(), cancelledAt)).isTrue();
        assertThat(vaccinationRepository.cancel(completed.getId(), cancelledAt)).isFalse();

        VaccinationEntity reloaded = vaccinationJpaRepository.findById(scheduled.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(VaccinationStatus.CANCELLED);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(cancelledAt);
        assertThat(vaccinationJpaRepository.findById(completed.getId()).orElseThrow().getStatus())
                .isEqualTo(VaccinationStatus.COMPLETED);
    }

    @Test
    @DisplayName("論理削除は未削除の行だけを対象とし、削除後は一覧・リマインダー対象から外れる")
    void soft_delete_only_active_row() {
        // given
        Long petId = persistPet(1L, null).getId();
        VaccinationEntity scheduled = persistVaccination(petId, VaccinationStatus.SCHEDULED, TODAY);
        LocalDateTime deletedAt = CHANGED_AT.plusDays(1);

        // when & then
        assertThat(vaccinationRepository.softDelete(scheduled.getId(), deletedAt)).isTrue();
        assertThat(vaccinationRepository.softDelete(scheduled.getId(), deletedAt.plusHours(1))).isFalse();

        assertThat(vaccinationJpaRepository.findById(scheduled.getId()).orElseThrow().getDeletedAt()).isEqualTo(deletedAt);
        assertThat(vaccinationRepository.findAllByPetId(petId)).isEmpty();
        assertThat(reminderTargetIds()).isEmpty();
    }

    @Test
    @DisplayName("SCHD-T001/T003/T004: 7日以内のSCHEDULEDだけを飼い主付きで抽出する")
    void find_reminder_targets_filters_status_and_period() {
        // given
        PetEntity pet = persistPet(1L, null);
        VaccinationEntity today = persistVaccination(pet.getId(), VaccinationStatus.SCHEDULED, TODAY);
        VaccinationEntity lastDay = persistVaccination(pet.getId(), VaccinationStatus.SCHEDULED, TODAY.plusDays(7));
        persistVaccination(pet.getId(), VaccinationStatus.SCHEDULED, TODAY.plusDays(8));
        persistVaccination(pet.getId(), VaccinationStatus.SCHEDULED, TODAY.minusDays(1));
        persistVaccination(pet.getId(), VaccinationStatus.COMPLETED, TODAY.plusDays(1));
        persistVaccination(pet.getId(), VaccinationStatus.CANCELLED, TODAY.plusDays(1));

        // when
        em.flush();
        List<VaccinationReminderTarget> targets = vaccinationRepository.findReminderTargets(TODAY, TODAY.plusDays(7));

        // then
        assertThat(targets).extracting(VaccinationReminderTarget::vaccinationId)
                .containsExactly(today.getId(), lastDay.getId());
        assertThat(targets.get(0).userId()).isEqualTo(1L);
        assertThat(targets.get(0).petName()).isEqualTo("Momo");
    }

    @Test
    @DisplayName("論理削除されたペットの接種予定は抽出しない")
    void find_reminder_targets_skips_deleted_pet() {
        // given
        Long deletedPetId = persistPet(1L, CHANGED_AT).getId();
        persistVaccination(deletedPetId, VaccinationStatus.SCHEDULED, TODAY);

        // when & then
        assertThat(reminderTargetIds()).isEmpty();
    }

    @Test
    @DisplayName("SCHD-T005: 送信済みにした予定は再び抽出せず、二度目の送信済み化は失敗する")
    void find_reminder_targets_skips_already_reminded() {
        // given
        Long petId = persistPet(1L, null).getId();
        VaccinationEntity vaccination = persistVaccination(petId, VaccinationStatus.SCHEDULED, TODAY.plusDays(3));
        em.flush();

        // when & then
        assertThat(vaccinationRepository.markReminded(vaccination.getId(), TODAY.plusDays(3))).isTrue();
        assertThat(vaccinationRepository.markReminded(vaccination.getId(), TODAY.plusDays(3))).isFalse();
        assertThat(reminderTargetIds()).isEmpty();
        assertThat(vaccinationJpaRepository.findById(vaccination.getId()).orElseThrow().getRemindedDate())
                .isEqualTo(TODAY.plusDays(3));
    }

    @Test
    @DisplayName("送信済みの予定を中止・再登録すると、新しい予定日で再び抽出される")
    void rescheduled_after_reminder_is_extracted_again() {
        // given
        Long petId = persistPet(1L, null).getId();
        VaccinationEntity vaccination = persistVaccination(petId, VaccinationStatus.SCHEDULED, TODAY.plusDays(3));
        em.flush();
        vaccinationRepository.markReminded(vaccination.getId(), TODAY.plusDays(3));

        // when
        vaccinationRepository.cancel(vaccination.getId(), CHANGED_AT.plusHours(1));
        vaccinationRepository.reschedule(vaccination.getId(), TODAY.plusDays(5), CHANGED_AT.plusHours(2));

        // then
        em.flush();
        assertThat(vaccinationRepository.findReminderTargets(TODAY, TODAY.plusDays(7)))
                .extracting(VaccinationReminderTarget::vaccinationId, VaccinationReminderTarget::scheduledDate)
                .containsExactly(tuple(vaccination.getId(), TODAY.plusDays(5)));
    }

    @Test
    @DisplayName("抽出後に中止・再登録が確定した場合、抽出時の予定日では送信済みにできず、新しい予定日は未送信のまま残る（REVIEW-001 R-03）")
    void mark_reminded_fails_for_target_rescheduled_after_extraction() {
        // given: 抽出時点では予定日 D1
        Long petId = persistPet(1L, null).getId();
        VaccinationEntity vaccination = persistVaccination(petId, VaccinationStatus.SCHEDULED, TODAY.plusDays(2));
        List<VaccinationReminderTarget> extracted = vaccinationRepository.findReminderTargets(TODAY, TODAY.plusDays(7));
        assertThat(extracted).hasSize(1);

        // when: 抽出後、送信前に D2 へ中止→再登録が確定
        vaccinationRepository.cancel(vaccination.getId(), CHANGED_AT.plusHours(1));
        vaccinationRepository.reschedule(vaccination.getId(), TODAY.plusDays(6), CHANGED_AT.plusHours(2));

        // then: 抽出した D1 では送信済みにできず、D2 は次回の抽出対象のまま
        assertThat(vaccinationRepository.markReminded(vaccination.getId(), extracted.get(0).scheduledDate())).isFalse();
        assertThat(vaccinationRepository.findReminderTargets(TODAY, TODAY.plusDays(7)))
                .extracting(VaccinationReminderTarget::scheduledDate)
                .containsExactly(TODAY.plusDays(6));
        assertThat(vaccinationRepository.markReminded(vaccination.getId(), TODAY.plusDays(6))).isTrue();
    }
}

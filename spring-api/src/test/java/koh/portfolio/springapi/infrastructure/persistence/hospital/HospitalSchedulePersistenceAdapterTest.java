package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalSchedule;
import koh.portfolio.springapi.domain.hospital.model.HospitalScheduleStatus;
import koh.portfolio.springapi.domain.hospital.model.HospitalStatus;
import koh.portfolio.springapi.domain.hospital.port.HospitalRepository;
import koh.portfolio.springapi.domain.hospital.port.HospitalScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 予約枠の自動生成で使う問い合わせ（既に枠がある日・一括保存・病院行の排他取得）を実DB（H2）で実行する
@DataJpaTest
@ActiveProfiles("test")
@Import({
        HospitalSchedulePersistenceAdapter.class,
        HospitalScheduleMapperImpl.class,
        HospitalPersistenceAdapter.class,
        HospitalMapperImpl.class
})
class HospitalSchedulePersistenceAdapterTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 20, 9, 0);

    @Autowired
    private HospitalScheduleRepository hospitalScheduleRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("期間内で枠がある日を重複なく返し、一括保存した枠（BLOCKEDを含む）もその日として数える")
    void find_dates_with_schedules_and_save_all() {
        // given
        LocalDate d1 = LocalDate.of(2026, 11, 2);
        LocalDate d2 = LocalDate.of(2026, 11, 3);
        hospitalScheduleRepository.saveAll(List.of(
                HospitalSchedule.create(1L, d1, LocalTime.of(9, 0), LocalTime.of(9, 30)),
                HospitalSchedule.create(1L, d1, LocalTime.of(9, 30), LocalTime.of(10, 0)).block(),
                HospitalSchedule.create(1L, LocalDate.of(2026, 12, 1), LocalTime.of(9, 0), LocalTime.of(9, 30)), // 期間外
                HospitalSchedule.create(2L, d2, LocalTime.of(9, 0), LocalTime.of(9, 30)))); // 別の病院
        em.flush();
        em.clear();

        // when
        List<LocalDate> dates = hospitalScheduleRepository.findDatesWithSchedules(1L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        // then
        assertThat(dates).containsExactly(d1);
        assertThat(hospitalScheduleRepository.findAllByHospitalId(1L))
                .extracting(HospitalSchedule::getStatus)
                .containsExactly(HospitalScheduleStatus.AVAILABLE, HospitalScheduleStatus.BLOCKED, HospitalScheduleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("病院行の排他取得は論理削除されていない病院だけを返す")
    void find_hospital_by_id_for_update() {
        // given
        Long active = em.persist(HospitalEntity.builder().name("A").address("Tokyo").status(HospitalStatus.ACTIVE)
                .createdAt(AT).updatedAt(AT).build()).getId();
        Long deleted = em.persist(HospitalEntity.builder().name("D").address("Tokyo").status(HospitalStatus.ACTIVE)
                .createdAt(AT).updatedAt(AT).deletedAt(AT).build()).getId();
        em.flush();

        // when & then
        assertThat(hospitalRepository.findByIdForUpdate(active)).hasValueSatisfying(h -> assertThat(h.getName()).isEqualTo("A"));
        assertThat(hospitalRepository.findByIdForUpdate(deleted)).isEmpty();
    }
}

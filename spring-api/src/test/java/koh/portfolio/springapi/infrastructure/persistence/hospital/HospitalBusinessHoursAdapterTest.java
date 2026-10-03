package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import koh.portfolio.springapi.domain.hospital.port.HospitalBusinessHoursRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        HospitalBusinessHoursAdapter.class,
        HospitalBusinessHoursMapperImpl.class
})
class HospitalBusinessHoursAdapterTest {

    @Autowired
    private HospitalBusinessHoursRepository hospitalBusinessHoursRepository;

    @Autowired
    private HospitalBusinessHoursJpaRepository hospitalBusinessHoursJpaRepository;

    private HospitalBusinessHoursEntity persist() {
        LocalDateTime now = LocalDateTime.now();
        return hospitalBusinessHoursJpaRepository.save(HospitalBusinessHoursEntity.builder()
                .hospitalId(1L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private HospitalBusinessHours changed(Long id, Long hospitalId) {
        LocalDateTime now = LocalDateTime.now();
        return new HospitalBusinessHours(id, hospitalId, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(19, 0),
                null, null, 20, now, now);
    }

    @Test
    @DisplayName("営業時間の時間列が更新される")
    void update_hours_writes_row() {
        // given
        HospitalBusinessHoursEntity saved = persist();

        // when
        boolean updated = hospitalBusinessHoursRepository.updateHours(changed(saved.getId(), 1L));

        // then
        assertThat(updated).isTrue();
        HospitalBusinessHoursEntity reloaded = hospitalBusinessHoursJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(reloaded.getSlotDurationMinutes()).isEqualTo(20);
    }

    @Test
    @DisplayName("削除済みの営業時間は更新されず、行も作り直されない")
    void update_hours_does_not_recreate_deleted_row() {
        // given
        HospitalBusinessHoursEntity saved = persist();
        hospitalBusinessHoursJpaRepository.deleteById(saved.getId());
        hospitalBusinessHoursJpaRepository.flush();

        // when: 削除前に読み取った内容で書き込もうとする
        boolean updated = hospitalBusinessHoursRepository.updateHours(changed(saved.getId(), 1L));

        // then
        assertThat(updated).isFalse();
        assertThat(hospitalBusinessHoursJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("別病院のhospitalIdを指定した更新は0件となる")
    void update_hours_rejects_other_hospital() {
        // given
        HospitalBusinessHoursEntity saved = persist();

        // when
        boolean updated = hospitalBusinessHoursRepository.updateHours(changed(saved.getId(), 2L));

        // then
        assertThat(updated).isFalse();
        assertThat(hospitalBusinessHoursJpaRepository.findById(saved.getId()).orElseThrow().getOpenTime())
                .isEqualTo(LocalTime.of(9, 0));
    }
}

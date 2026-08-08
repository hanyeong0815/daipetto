package koh.portfolio.springapi.infrastructure.persistence.hospital;

import koh.portfolio.springapi.domain.hospital.model.HospitalBusinessHours;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class HospitalBusinessHoursMapperTest {

    private final HospitalBusinessHoursMapper hospitalBusinessHoursMapper = Mappers.getMapper(HospitalBusinessHoursMapper.class);

    @Test
    @DisplayName("DomainをEntityに変換できる")
    void domain_to_entity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        HospitalBusinessHours domain = new HospitalBusinessHours(1L, 10L, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(18, 0), LocalTime.of(12, 0), LocalTime.of(13, 0), 30, now, now);

        // when
        HospitalBusinessHoursEntity entity = hospitalBusinessHoursMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getHospitalId()).isEqualTo(10L);
        assertThat(entity.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(entity.getOpenTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(entity.getCloseTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(entity.getBreakStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(entity.getBreakEndTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(entity.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("EntityをDomainに変換できる")
    void entity_to_domain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        HospitalBusinessHoursEntity entity = HospitalBusinessHoursEntity.builder()
                .id(1L)
                .hospitalId(10L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .breakStartTime(LocalTime.of(12, 0))
                .breakEndTime(LocalTime.of(13, 0))
                .slotDurationMinutes(30)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        HospitalBusinessHours domain = hospitalBusinessHoursMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getHospitalId()).isEqualTo(10L);
        assertThat(domain.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(domain.getOpenTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(domain.getCloseTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(domain.getBreakStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(domain.getBreakEndTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(domain.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(domain.getCreatedAt()).isEqualTo(now);
    }
}
